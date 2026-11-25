package com.oddlabs.tt.engine.render.state;

import java.nio.ByteBuffer;
import org.jspecify.annotations.NullMarked;

/// Supplies water uniform data packed into a UBO buffer.
@FunctionalInterface
@NullMarked
public interface WaterUniformsProvider {
  void putGlobalUniforms(ByteBuffer buffer, boolean enableWaves);
}
