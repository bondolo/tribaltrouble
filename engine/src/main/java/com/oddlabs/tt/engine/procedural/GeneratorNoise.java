package com.oddlabs.tt.engine.procedural;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.TextureGenerator;
import com.oddlabs.tt.procedural.image.NoiseProcedural;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * A texture generator that produces Perlin noise.
 * Used for organic turbulence effects in shaders.
 */
public final class GeneratorNoise extends TextureGenerator {
    private final int size;
    private final long seed;

    public GeneratorNoise(int size, long seed) {
        this.size = size;
        this.seed = seed;
    }

    @Override
    public Texture[] generate() {
        Layer layer = NoiseProcedural.generate(size, seed);
        return new Texture[]{
                new Texture(new GLIntImage(layer), GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT,
                        GL11.GL_REPEAT),
        };
    }

    @Override
    public int hashCode() {
        return size + (int) seed;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof GeneratorNoise other && size == other.size && seed == other.seed;
    }
}
