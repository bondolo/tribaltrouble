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
    protected volatile @Nullable OGGStream ogg_stream;
    protected volatile int channels;

    static void stopAll() {
        queued_players.forEach(QueuedAudioPlayer::stop);
    }

    protected QueuedAudioPlayer(@Nullable AudioSource source, float x, float y, float z, @NonNull AudioParameters<@NonNull String> params, int numBuffers) {
        super(source, x, y, z, params);
        if (!isPlaying() || this.source == null) {
            this.ogg_stream = null;
            this.channels = 0;
            return;
        }

        Thread.startVirtualThread(() -> {
            try {
                this.ogg_stream = new OGGStream(Utils.makeURL(params.sound()));
                this.channels = ogg_stream.getChannels();

                initAsync();

                // Calculate sleep interval based on total queued time across all buffers.
                // We wait for approximately half of the total buffers to be empty before waking up.
                long totalSamplesPerChannel = (long) PCM_SAMPLES * numBuffers / channels;
                long sleepInterval = Math.max(10, (1000L * totalSamplesPerChannel / ogg_stream.getRate()) / 2);

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
            } catch (Exception e) {
                // Failed to load, init, or read. Exit silently.
            } finally {
                cleanup();
            }
        });

        queued_players.add(this);
    }

    /** Run by the Refiller thread */
    protected abstract void initAsync() throws Exception;
    
    /** Run by the Refiller thread */
    public abstract void refill() throws IOException;

    /** Run by the Refiller thread */
    protected abstract void cleanupAsync();

    protected int readPCM() {
        pcmBuffer.clear(); // Position 0, Limit PCM_SAMPLES
        var stream = ogg_stream;
        if (stream == null) return 0;

        int shortsRead = stream.read(pcmBuffer);

        if (shortsRead <= 0 && getParameters().looping()) {
            // End of ogg stream reached, but we are looping.
            stream.seek(0);
            shortsRead = stream.read(pcmBuffer);
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
    public @NonNull QueuedAudioPlayer stop() {
        super.stop(); // Sets playing = false and stops the source.
        queued_players.remove(this);
        // The virtual thread will see playing == false and exit, running its finally block to cleanup.
        return this;
    }

    private void cleanup() {
        OGGStream stream = ogg_stream;
        ogg_stream = null;
        if (null != stream) {
            stream.close();
        }
        cleanupAsync();
    }
}
