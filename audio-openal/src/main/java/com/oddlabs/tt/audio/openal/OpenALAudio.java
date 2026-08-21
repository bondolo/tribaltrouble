package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.OGGStream;
import com.oddlabs.tt.base.resource.NativeResource;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.oddlabs.tt.audio.openal.OpenALManager.checkALError;

/**
 * OpenAL buffered audio
 */
final class OpenALAudio extends NativeResource<OpenALAudio.Buffers> implements Audio {
    static final class Buffers extends NativeState {
        private final IntBuffer al_buffers;

        Buffers(int num_buffers) {
            al_buffers = BufferUtils.createIntBuffer(num_buffers);
            AL10.alGenBuffers(al_buffers);
            checkALError("alGenBuffers " + num_buffers);
        }

        @Override
        public void close() {
            if (ALC10.alcGetCurrentContext() != 0 && al_buffers.limit() > 0) {
                AL10.alGetError(); // Clear any sticky error from previous operations
                AL10.alDeleteBuffers(al_buffers);
                checkALError("alDeleteBuffers");
            }
            al_buffers.limit(0);
        }
    }

    OpenALAudio(OpenALManager manager, int num_buffers) {
        super(new Buffers(num_buffers), manager::enqueueCleanup);
    }

    OpenALAudio(OpenALManager manager, URL file) throws IOException {
        this(manager, 1);
        try {
            Wave wave = new Wave(file);
            AL10.alBufferData(getBuffer(), wave.getFormat(), wave.getData(), wave.getSampleRate());
        } catch (UnsupportedAudioFileException e) {
            // Assume it's an ogg vorbis file
            loadOGG(file, getBuffer());
        }
    }

    private static void loadOGG(URL file, int bufferId) throws IOException {
        try (OGGStream stream = new OGGStream(file)) {
            int channels = stream.getChannels();
            int sampleRate = stream.getRate();
            int lengthInSamples = stream.getLengthInSamples();
            if (lengthInSamples <= 0) {
                throw new IOException("Failed to determine OGG stream length: " + file);
            }
            ShortBuffer pcm = BufferUtils.createShortBuffer(lengthInSamples * channels);
            int samplesRead = stream.read(pcm);
            if (samplesRead != lengthInSamples * channels) {
                throw new IOException("Failed to read all OGG samples: " + file);
            }
            pcm.flip();
            int format = Wave.getFormat(channels, Short.SIZE);
            AL10.alBufferData(bufferId, format, pcm, sampleRate);
        }
    }

    int getBufferCount() {
        return state.al_buffers.remaining();
    }

    IntBuffer getBuffers() {
        return state.al_buffers.duplicate().position(0);
    }

    int getBuffer() {
        return getBuffer(0);
    }

    int getBuffer(int idx) {
        return state.al_buffers.get(idx);
    }
}
