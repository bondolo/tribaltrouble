package com.oddlabs.tt.audio;

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

final class OGGStreamTest {

    @Test
    void testOpenAndReadOggStream() throws Exception {
        List<File> oggFiles = new ArrayList<>();
        File sfxDir = new File("../content/src/main/resources/sfx");
        if (!sfxDir.exists()) {
            sfxDir = new File("content/src/main/resources/sfx");
        }
        File musicDir = new File("../content/src/main/resources/music");
        if (!musicDir.exists()) {
            musicDir = new File("content/src/main/resources/music");
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
                assertTrue(stream.getChannels() > 0);
                assertTrue(stream.getRate() > 0);
                int lengthInSamples = stream.getLengthInSamples();
                assertTrue(lengthInSamples > 0, "Length in samples should be > 0 for " + file.getName());

                ShortBuffer pcm = BufferUtils.createShortBuffer(lengthInSamples * stream.getChannels());
                int read = stream.read(pcm);
                assertEquals(lengthInSamples * stream.getChannels(), read, "Read count mismatch for " + file.getName());

                // Test seek and partial read
                stream.seek(0);
                ShortBuffer chunk = BufferUtils.createShortBuffer(1024);
                int chunkRead = stream.read(chunk);
                assertTrue(chunkRead > 0);
            }
        }
    }
}
