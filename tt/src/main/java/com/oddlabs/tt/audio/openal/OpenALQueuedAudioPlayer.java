package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.audio.OGGStream;
import com.oddlabs.tt.audio.QueuedAudioPlayer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryStack;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * OpenAL implementation of {@link QueuedAudioPlayer} using a multi-buffer queue for streaming.
 */
final class OpenALQueuedAudioPlayer extends QueuedAudioPlayer {
    private static final Logger logger = Logger.getLogger(OpenALQueuedAudioPlayer.class.getSimpleName());
    private static final int NUM_BUFFERS = 12;
    private volatile int al_format;
    private volatile int al_rate;

    OpenALQueuedAudioPlayer(@Nullable OpenALAudioSource source, float x, float y, float z, @NonNull AudioParameters params) {
        super(source, x, y, z, params);
    }

    @Override
    public @NonNull QueuedAudioPlayer stop() {
        synchronized (this) {
            this.notifyAll(); // Wake up the filler thread.
            // The filler thread will see playing == false and exit, running its finally block to cleanup.
        }
        return super.stop();
    }

    @Override
    protected @Nullable OpenALAudio initAsync(@NonNull OGGStream stream) {
        if (this.source == null || !isPlaying()) {
            this.al_format = AL10.AL_NONE;
            return null;
        }

        this.al_format = Wave.getFormat(stream.getChannels(), Short.SIZE);
        this.al_rate = stream.getRate();

        long bufferduration = (long) NUM_BUFFERS * (PCM_SAMPLES / stream.getChannels()) / (al_rate / TimeUnit.SECONDS.toMillis(1));
        logger.info("Creating OpenAL audio with " + NUM_BUFFERS + " buffers for " + Duration.ofMillis(bufferduration));
        var audio = new OpenALAudio((OpenALManager) Renderer.getRenderer().getAudioManager(), NUM_BUFFERS);

        Utils.toIntStream(audio.getBuffers()).forEachOrdered(i -> fillBuffer(i, stream));

        ((OpenALAudioSource) source).queue(audio.getBuffers());

        return audio;
    }

    @Override
    protected int getBufferCount() {
        Audio audio = this.audio;
        assert audio != null : "Audio not initialized";
        return ((OpenALAudio)audio).getBufferCount();
    }

    private void fillBufferFromStream(int al_buffer) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        AL10.alBufferData(al_buffer, al_format, pcmBuffer, al_rate);
    }

    private int fillBuffer(int al_buffer, @NonNull OGGStream stream) {
        int shortsRead = readPCM(stream);
        if (shortsRead > 0) {
            fillBufferFromStream(al_buffer);
        }
        return shortsRead;
    }

    @Override
    public void refill(@NonNull OGGStream stream) {
        if (source == null || !isPlaying() || ALC10.alcGetCurrentContext() == 0) return;
        int processed = ((OpenALAudioSource) source).processed();
        try (var stack = MemoryStack.stackPush()) {
            var al_return_buffers = stack.mallocInt(1);
            while (processed > 0 && isPlaying()) {
                if (ALC10.alcGetCurrentContext() == 0) return;
                ((OpenALAudioSource) source).unqueued(al_return_buffers);
                int bytes = fillBuffer(al_return_buffers.get(0), stream);
                if (bytes == 0) {
                    stop();
                    return;
                }
                if (isPlaying()) {
                    ((OpenALAudioSource) source).queue(al_return_buffers);
                }
                processed--;
            }
        }

        if (isPlaying() && source.getState() != AudioSource.State.PLAYING) {
            source.play();
        }
    }

    @Override
    protected void cleanupAsync() throws Exception {
        if (audio instanceof AutoCloseable toClose) {
            toClose.close();
        }
    }
}
