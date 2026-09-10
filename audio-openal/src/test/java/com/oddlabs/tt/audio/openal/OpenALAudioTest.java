package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.OGGStream;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.net.URL;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OpenALAudioTest {

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
