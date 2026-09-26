package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.OGGStream;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

import java.io.File;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OpenALAudioTest {

    @Test
    void testOpenALSourceTransitionBetweenStaticAndStreaming() {
        long device = ALC10.alcOpenDevice((String) null);
        if (device == 0) return;
        long context = ALC10.alcCreateContext(device, (int[]) null);
        ALC10.alcMakeContextCurrent(context);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        AL.createCapabilities(alcCapabilities);

        try {
            int source = AL10.alGenSources();
            int staticBuffer = AL10.alGenBuffers();
            ShortBuffer pcm = BufferUtils.createShortBuffer(1024);
            AL10.alBufferData(staticBuffer, AL10.AL_FORMAT_MONO16, pcm, 44100);

            // 1. Simulate playing a static SFX on the pooled source
            AL10.alSourcei(source, AL10.AL_BUFFER, staticBuffer);
            assertEquals(AL11.AL_STATIC, AL10.alGetSourcei(source, AL11.AL_SOURCE_TYPE));
            // In OpenAL 1.1, AL_BUFFERS_QUEUED is 1 even for static single-buffer attachments
            assertEquals(1, AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED));

            // 2. Transition from static to streaming:
            // Since AL_SOURCE_TYPE is AL_STATIC (not AL_STREAMING), detach static buffer
            if (AL10.alGetSourcei(source, AL11.AL_SOURCE_TYPE) != AL11.AL_STREAMING) {
                AL10.alSourceStop(source);
                AL10.alSourcei(source, AL10.AL_BUFFER, AL10.AL_NONE);
            }
            assertEquals(AL11.AL_UNDETERMINED, AL10.alGetSourcei(source, AL11.AL_SOURCE_TYPE));
            assertEquals(0, AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED));

            // 3. Queue streaming buffers (e.g. music)
            IntBuffer queueBuffers = BufferUtils.createIntBuffer(2);
            AL10.alGenBuffers(queueBuffers);
            AL10.alBufferData(queueBuffers.get(0), AL10.AL_FORMAT_MONO16, pcm, 44100);
            AL10.alBufferData(queueBuffers.get(1), AL10.AL_FORMAT_MONO16, pcm, 44100);

            AL10.alSourceQueueBuffers(source, queueBuffers);
            assertEquals(0, AL10.alGetError(), "alSourceQueueBuffers should succeed after detaching static buffer");
            assertEquals(AL11.AL_STREAMING, AL10.alGetSourcei(source, AL11.AL_SOURCE_TYPE));
            assertEquals(2, AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED));

            // 4. Transition from streaming back to static
            OpenALAudioSource.detachBuffers(source);
            assertEquals(AL11.AL_UNDETERMINED, AL10.alGetSourcei(source, AL11.AL_SOURCE_TYPE));
            assertEquals(0, AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED));

            AL10.alSourcei(source, AL10.AL_BUFFER, staticBuffer);
            assertEquals(0, AL10.alGetError(), "alSourcei AL_BUFFER should succeed after detaching queue");
            assertEquals(AL11.AL_STATIC, AL10.alGetSourcei(source, AL11.AL_SOURCE_TYPE));

            AL10.alDeleteSources(source);
            AL10.alDeleteBuffers(staticBuffer);
            AL10.alDeleteBuffers(queueBuffers);
        } finally {
            ALC10.alcMakeContextCurrent(0);
            ALC10.alcDestroyContext(context);
            ALC10.alcCloseDevice(device);
        }
    }

    @Test
    void testDecodeAllOggFiles() throws Exception {
        List<File> oggFiles = new ArrayList<>();
        File sfxDir = new File("../assets/sfx");
        if (!sfxDir.exists()) {
            sfxDir = new File("assets/sfx");
        }
        File musicDir = new File("../assets/music");
        if (!musicDir.exists()) {
            musicDir = new File("assets/music");
        }

        File[] sfxList = sfxDir.listFiles((_, name) -> name.endsWith(".ogg"));
        if (sfxList != null) {
            Collections.addAll(oggFiles, sfxList);
        }
        File[] musicList = musicDir.listFiles((_, name) -> name.endsWith(".ogg"));
        if (musicList != null) {
            Collections.addAll(oggFiles, musicList);
        }

        assertFalse(oggFiles.isEmpty(), "OGG files should be found");

        for (File file : oggFiles) {
            URL url = file.toURI().toURL();
            try (OGGStream stream = new OGGStream(url)) {
                int channels = stream.getChannels();
                int sampleRate = stream.getRate();
                int totalSamples = stream.getLengthInSamples();
                assertTrue(channels > 0);
                assertTrue(sampleRate > 0);
                assertTrue(totalSamples > 0, "Total samples should be > 0 for " + file.getName());

                ShortBuffer pcm = BufferUtils.createShortBuffer(totalSamples * channels);
                int read = stream.read(pcm);
                assertEquals(totalSamples * channels, read, "Should read all samples from " + file.getName());
            }
        }
    }
}
