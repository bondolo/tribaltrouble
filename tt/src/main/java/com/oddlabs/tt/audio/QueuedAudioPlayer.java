package com.oddlabs.tt.audio;

import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.ShortBuffer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An audio player that streams audio data from an OGG stream into multiple queued buffers.
 */
public abstract class QueuedAudioPlayer extends AudioPlayer {
    private static final Logger logger = Logger.getLogger(QueuedAudioPlayer.class.getSimpleName());
    private static final int PCM_SAMPLES = 16384;

    protected final ShortBuffer pcmBuffer = BufferUtils.createShortBuffer(PCM_SAMPLES);
    protected volatile @Nullable OGGStream ogg_stream;

    protected QueuedAudioPlayer(@Nullable AudioSource source, float x, float y, float z, @NonNull AudioParameters<@NonNull AudioFile> params, int numBuffers) {
        super(source, x, y, z, params);
        if (!isPlaying() || this.source == null) {
            this.ogg_stream = null;
            return;
        }

        Thread.startVirtualThread(() -> refiller(params.sound().getURL(), numBuffers));
    }

    private void refiller(@NonNull URL source, int numBuffers) {
        try {
            var stream = new OGGStream(source);
            int channels = stream.getChannels();
            int rate = stream.getRate();
            ogg_stream = stream;

            initAsync(channels);

            // Calculate the sleep interval based on total queued time across all buffers.
            // We wait for approximately half of the total buffers to be empty before waking up.
            long totalSamplesPerChannel = (long) PCM_SAMPLES * numBuffers / channels;
            long sleepInterval = Math.max(10, (TimeUnit.SECONDS.toMillis(1) * totalSamplesPerChannel / rate) / 2);

            while (isPlaying()) {
                try {
                    long start = System.currentTimeMillis();
                    refill();
                    var sleep = sleepInterval - (start - System.currentTimeMillis());
                    if (sleep > 0) {
                        //noinspection BusyWait
                        Thread.sleep(sleep);
                    }
                } catch (InterruptedException | IOException e) {
                    break;
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Failed to read OGG stream " + source, ioe);
        } catch (Exception _) {
            // Failed to load, init, or read. Exit silently.
        } finally {
            cleanup();
        }
    }

    /** Run by the Refiller thread */
    protected abstract void initAsync(int channels) throws Exception;
    
    /** Run by the Refiller thread */
    protected abstract void refill() throws IOException;

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
        if (Renderer.getRenderer().getAudioManager().removeQueuedPlayer(this)) {
            super.stop(); // Sets playing = false and stops the source.
            // The filler thread will see playing == false and exit, running its finally block to cleanup.
        }

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
