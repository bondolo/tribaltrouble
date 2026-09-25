package com.oddlabs.tt.engine.render.state;

import org.jspecify.annotations.NullMarked;

import java.nio.ByteBuffer;

/**
 * Supplies water uniform data packed into a UBO buffer.
 */
@FunctionalInterface
@NullMarked
public interface WaterUniformsProvider {
    /**
     * Packs water uniforms into the specified buffer.
     *
     * @param buffer the destination buffer
     * @param enableWaves whether wave simulation parameters are active
     */
    void putGlobalUniforms(ByteBuffer buffer, boolean enableWaves);
}
