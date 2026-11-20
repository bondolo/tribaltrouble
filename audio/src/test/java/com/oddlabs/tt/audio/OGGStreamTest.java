package com.oddlabs.tt.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

final class OGGStreamTest {

  @Test
  void testOpenAndReadOggStream() throws Exception {
    List<File> oggFiles = new ArrayList<>();
    File sfxDir = resolveDir("../assets/sfx", "assets/sfx");
    File musicDir = resolveDir("../assets/music", "assets/music");
    collectOggFiles(oggFiles, sfxDir, musicDir);

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

  /// Reading a stream in sequential chunks must reconstruct the full sample data and advance the buffer position by the
  /// number of shorts actually written.
  @Test
  void testSequentialStreamRead() throws Exception {
    File file = findFirstOggFile();
    try (OGGStream stream = new OGGStream(file.toURI().toURL())) {
      int channels = stream.getChannels();
      int lengthInSamples = stream.getLengthInSamples();
      int total = lengthInSamples * channels;

      // Reference decode: a single read into a full-size buffer.
      ShortBuffer full = BufferUtils.createShortBuffer(total);
      int fullRead = stream.read(full);
      assertEquals(total, fullRead, "Full read count mismatch for " + file.getName());
      short[] expected = new short[total];
      full.flip();
      full.get(expected);

      // Sequential decode: read in small chunks from position 0 onward.
      stream.seek(0);
      int chunkShorts = 128;
      short[] actual = new short[total];
      ShortBuffer chunk = BufferUtils.createShortBuffer(chunkShorts);
      int accumulated = 0;
      while (accumulated < total) {
        int n = stream.read(chunk);
        assertTrue(
            n > 0, "read() returned 0 with " + (total - accumulated) + " shorts remaining for " + file.getName());
        assertTrue(chunk.position() == n, "Buffer position must advance by shorts read for " + file.getName());
        chunk.flip();
        chunk.get(actual, accumulated, n);
        accumulated += n;
        chunk.clear();
      }

      assertEquals(total, accumulated, "Sequential read total mismatch for " + file.getName());
      assertArrayEquals(expected, actual, "Sequential read samples differ from single read for " + file.getName());
    }
  }

  /**
   * Reproduces the looping wrap-around path used by QueuedAudioPlayer#readPCM: when the clip is shorter than the PCM
   * buffer, the first read leaves the buffer partially filled, then seek(0) and a second read top it up from a non-zero
   * position. This pins both the relaxed hasRemaining() assertion and the position-accumulating advance in
   * OGGStream#read (which would throw / discard data under the old position()==0 contract).
   */
  @Test
  void testLoopWrapRead() throws Exception {
    File file = findShortOggFile();
    try (OGGStream stream = new OGGStream(file.toURI().toURL())) {
      int channels = stream.getChannels();
      int lengthInSamples = stream.getLengthInSamples();
      int total = lengthInSamples * channels;

      int capacity = Math.max(total * 3, 1024 * channels);

      ShortBuffer buffer = BufferUtils.createShortBuffer(capacity);
      int shortsRead = stream.read(buffer);
      assertEquals(total, shortsRead, "First read must consume the whole short clip for " + file.getName());
      assertTrue(shortsRead == buffer.position(), "read() must advance the buffer position");
      assertTrue(buffer.hasRemaining(), "First read must leave room in the buffer for the wrap read");

      // Wrap to the beginning and fill the remainder — exactly the loop path.
      int positionBefore = buffer.position();
      stream.seek(0);
      int more = stream.read(buffer);
      assertTrue(more > 0, "Wrap read must produce samples for " + file.getName());
      assertEquals(total, more, "Wrap read must consume the whole short clip for " + file.getName());
      // The position must accumulate across the two reads (the behaviour the diff fixes);
      // it must not reset to `more` the way the old position(samplesRead*channels) contract did.
      assertEquals(positionBefore + more, buffer.position(), "read() must accumulate the buffer position");

      // The wrapped remainder is the same as the very first block of the clip.
      short[] head = new short[shortsRead];
      stream.seek(0);
      ShortBuffer headBuffer = BufferUtils.createShortBuffer(shortsRead);
      stream.read(headBuffer);
      headBuffer.flip();
      headBuffer.get(head);
      short[] wrapped = new short[more];
      buffer.position(shortsRead);
      buffer.get(wrapped);
      assertArrayEquals(head, wrapped, "Wrapped remainder must equal the clip's beginning for " + file.getName());
    }
  }

  private static File resolveDir(String relative, String fallback) {
    File dir = new File(relative);
    return dir.exists() ? dir : new File(fallback);
  }

  private static void collectOggFiles(List<File> target, File sfxDir, File musicDir) {
    File[] sfxList = sfxDir.listFiles((_, name) -> name.endsWith(".ogg"));
    if (sfxList != null) {
      Collections.addAll(target, sfxList);
    }
    File[] musicList = musicDir.listFiles((_, name) -> name.endsWith(".ogg"));
    if (musicList != null) {
      Collections.addAll(target, musicList);
    }
  }

  private static File findFirstOggFile() throws Exception {
    List<File> oggFiles = new ArrayList<>();
    collectOggFiles(oggFiles, resolveDir("../assets/sfx", "assets/sfx"), resolveDir("../assets/music", "assets/music"));
    assertFalse(oggFiles.isEmpty(), "OGG files should be found");
    return oggFiles.get(0);
  }

  /// Returns an OGG clip shorter than a typical PCM buffer, so that a first read leaves the buffer partially filled and
  /// a wrap-around read is required.
  private static File findShortOggFile() throws Exception {
    List<File> oggFiles = new ArrayList<>();
    collectOggFiles(oggFiles, resolveDir("../assets/sfx", "assets/sfx"), resolveDir("../assets/music", "assets/music"));
    assertFalse(oggFiles.isEmpty(), "OGG files should be found");
    File chosen = oggFiles.get(0);
    for (File file : oggFiles) {
      try (OGGStream stream = new OGGStream(file.toURI().toURL())) {
        if (stream.getLengthInSamples() * stream.getChannels() < 512 * stream.getChannels()) {
          chosen = file;
          break;
        }
      }
    }
    assertNotNull(chosen, "A short OGG file must be available for the loop-wrap test");
    return chosen;
  }
}
