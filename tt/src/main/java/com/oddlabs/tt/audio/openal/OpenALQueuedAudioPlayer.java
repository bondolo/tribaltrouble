package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.audio.QueuedAudioPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.io.IOException;
import java.nio.IntBuffer;

/**
 * OpenAL implementation of {@link QueuedAudioPlayer} using a multi-buffer queue for streaming.
 */
final class OpenALQueuedAudioPlayer extends QueuedAudioPlayer {
    private static final int NUM_BUFFERS = 12;
    private volatile @Nullable OpenALAudio audio;
    private final IntBuffer al_return_buffers = BufferUtils.createIntBuffer(1);
    private volatile int al_format;

    OpenALQueuedAudioPlayer(@Nullable OpenALAudioSource source, @NonNull AudioParameters<@NonNull String> params) {
        super(source, params, NUM_BUFFERS);
    }

    @Override
    protected void initAsync() {
        if (this.ogg_stream == null || this.source == null || !isPlaying()) {
            this.al_format = AL10.AL_NONE;
            this.audio = null;
            return;
        }

        this.audio = new OpenALAudio(NUM_BUFFERS);
        this.al_format = Wave.getFormat(channels, Short.SIZE);

        IntBuffer al_buffers = audio.getBuffers();
        for (int i = 0; i < al_buffers.capacity(); i++) {
            fillBuffer(al_buffers.get(i));
        }

        // Queued audio does not loop via source setting, it loops internally during buffer refill
        source.setLooping(false);
        ((OpenALAudioSource) source).queue(al_buffers);

        if (getParameters().music || AudioManager.getManager().startPlaying())
            source.play();
    }

    private void fillBufferFromStream(int al_buffer) {
        // alBufferData copies pcmBuffer.remaining() * 2 bytes of data into the OpenAL buffer.
        // The internal OpenAL buffer is automatically sized to match this byte count.
        var stream = ogg_stream;
        var format = al_format;
        if (stream != null) {
            AL10.alBufferData(al_buffer, format, pcmBuffer, stream.getRate());
        }
    }

    private int fillBuffer(int al_buffer) {
        int shortsRead = readPCM();
        if (shortsRead > 0) {
            fillBufferFromStream(al_buffer);
        }
        return shortsRead;
    }

    @Override
    public void refill() {
        if (source == null) return;
        int processed = ((OpenALAudioSource) source).processed();
        while (processed > 0) {
            ((OpenALAudioSource) source).unqueued(al_return_buffers);
            int bytes = fillBuffer(al_return_buffers.get(0));
            if (bytes == 0) {
                stop();
                return;
            }
            ((OpenALAudioSource) source).queue(al_return_buffers);
            processed--;
        }

        if (source.getState() == AudioSource.State.STOPPED && isPlaying()) {
            source.play();
        }
    }

    @Override
    protected void cleanupAsync() {
        OpenALAudio toClose = audio;
        audio = null;
        if (toClose != null) {
            toClose.close();
        }
    }
}
