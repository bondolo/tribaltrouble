package com.oddlabs.tt.audio;

import com.oddlabs.tt.base.resource.NativeResource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisAlloc;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

/// A stream used to decode OGG Vorbis audio data.
public final class OGGStream extends NativeResource<OGGStream.Decoder> {

  /// Native `stb_vorbis` decoder state and backing direct buffers.
  protected static class Decoder extends NativeResource.NativeState {
    private static final int ALLOC_BUFFER_SIZE = 256 * 1024;

    // Retain direct byte buffers so they are not garbage collected while the native decoder is active.
    @SuppressWarnings("FieldCanBeLocal")
    private final ByteBuffer decoderData;

    @SuppressWarnings("FieldCanBeLocal")
    private final ByteBuffer allocBuffer;

    private final long decoder;
    private final int channels;
    private final int sampleRate;

    private Decoder(byte[] vorbis) throws IOException {
      decoderData = BufferUtils.createByteBuffer(vorbis.length);
      decoderData.put(vorbis);
      decoderData.flip();

      allocBuffer = BufferUtils.createByteBuffer(ALLOC_BUFFER_SIZE);

      try (MemoryStack stack = MemoryStack.stackPush()) {
        STBVorbisAlloc alloc = STBVorbisAlloc.malloc(stack);
        alloc.alloc_buffer(allocBuffer);

        IntBuffer error = stack.mallocInt(1);
        decoder = STBVorbis.stb_vorbis_open_memory(decoderData, error, alloc);
        if (decoder == 0) {
          throw new IOException("Failed to open OGG Vorbis file. Error: " + error.get(0));
        }

        STBVorbisInfo info = STBVorbisInfo.malloc(stack);
        STBVorbis.stb_vorbis_get_info(decoder, info);
        this.channels = info.channels();
        this.sampleRate = info.sample_rate();
      }
    }

    @Override
    public void close() {
      STBVorbis.stb_vorbis_close(decoder);
    }
  }

  public OGGStream(URL source) throws IOException {
    this(source.openStream());
  }

  /// Reads OGG data from `stream` and closes `stream` in all cases.
  public OGGStream(InputStream stream) throws IOException {
    byte[] bytes;
    try (stream) {
      bytes = stream.readAllBytes();
    }
    this(bytes);
  }

  public OGGStream(byte[] bytes) throws IOException {
    super(new Decoder(bytes));
  }

  public int getChannels() {
    return state.channels;
  }

  public int getRate() {
    return state.sampleRate;
  }

  public int getLengthInSamples() {
    return STBVorbis.stb_vorbis_stream_length_in_samples(state.decoder);
  }

  public void seek(int sample) {
    STBVorbis.stb_vorbis_seek(state.decoder, sample);
  }

  /// Decodes interleaved samples directly into `buffer` and advances its position.
  ///
  /// @param buffer direct destination buffer with remaining space
  /// @return number of short values written to `buffer`
  public int read(ShortBuffer buffer) {
    assert buffer.hasRemaining() : "Buffer must have remaining space";
    int samplesRead = STBVorbis.stb_vorbis_get_samples_short_interleaved(state.decoder, state.channels, buffer);
    buffer.position(buffer.position() + samplesRead * state.channels);
    return samplesRead * state.channels;
  }

  @Override
  public void close() {
    super.close();
  }
}
