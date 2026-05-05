package com.oddlabs.tt.audio;

import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An audio player that streams audio data from an OGG stream into multiple queued buffers.
 */
public abstract class QueuedAudioPlayer extends AudioPlayer {
    private static final int PCM_SAMPLES = 16384;
    private static final Set<QueuedAudioPlayer> queued_players = new CopyOnWriteArraySet<>();

    protected final ShortBuffer pcmBuffer = org.lwjgl.BufferUtils.createShortBuffer(PCM_SAMPLES);
    protected final int channels;
    protected final @Nullable OGGStream ogg_stream;

    static void stopAll() {
        queued_players.forEach(QueuedAudioPlayer::stop);
    }

    protected QueuedAudioPlayer(@Nullable AudioSource source, @NonNull AudioParameters<@NonNull String> params, int numBuffers) throws IOException {
        super(source, params);
        if (!isPlaying() || this.source == null) {
            this.ogg_stream = null;
            this.channels = 0;
            return;
        }

        this.ogg_stream = new OGGStream(Utils.makeURL(params.sound));
        this.channels = ogg_stream.getChannels();

        // Calculate sleep interval based on total queued time across all buffers.
        // We wait for approximately half of the total buffers to be empty before waking up.
        long totalSamplesPerChannel = (long) PCM_SAMPLES * numBuffers / channels;
        long sleepInterval = Math.max(10, (1000L * totalSamplesPerChannel / ogg_stream.getRate()) / 2);

        Thread.startVirtualThread(() -> {
            try {
                while(isPlaying()) {
                    try {
                        refill();
                        Thread.sleep(sleepInterval);
                    } catch (InterruptedException e) {
                        break;
                    } catch (IOException e) {
                        break;
                    }
                }
            } finally {
                stop();
            }
        });

        queued_players.add(this);
    }

    /** Run by the Refiller thread */
    public abstract void refill() throws IOException;

    protected int readPCM() {
        pcmBuffer.clear(); // Position 0, Limit PCM_SAMPLES
        if (ogg_stream == null) return 0;

        int shortsRead = ogg_stream.read(pcmBuffer);

        if (shortsRead <= 0 && getParameters().looping) {
            // End of ogg stream reached, but we are looping.
            ogg_stream.seek(0);
            shortsRead = ogg_stream.read(pcmBuffer);
        }

        if (shortsRead > 0) {
            // Explicitly set the buffer's position and limit for OpenAL.
            // Some native wrappers might not update the buffer position automatically.
            pcmBuffer.position(0);
            pcmBuffer.limit(shortsRead);
        } else {
            pcmBuffer.position(0);
            pcmBuffer.limit(0);
        }

        return shortsRead;
    }

    @Override
    public void stop() {
        queued_players.remove(this);
        if (isPlaying()) {
            if (ogg_stream != null) {
                ogg_stream.close();
            }
            super.stop();
        }
    }
}
