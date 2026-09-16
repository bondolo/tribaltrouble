package com.oddlabs.tt.engine.procedural;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.TextureGenerator;
import com.oddlabs.tt.procedural.image.SmokeProcedural;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.Objects;

/**
 * Procedural texture generator for stylized particle smoke and damage smoke puffs.
 */
public final class GeneratorSmoke extends TextureGenerator {
    private static final int TEXTURE_SIZE = SmokeProcedural.TEXTURE_SIZE;

    private final int seed;
    private final float baseBrightness;
    private final float alphaMultiplier;

    /**
     * Standard smoke.
     */
    public GeneratorSmoke() {
        this(42, 0.6f, 1.0f);
    }

    /**
     * Parameterized constructor for specialized effects.
     *
     * @param seed random seed for noise.
     * @param baseBrightness base grayscale value [0, 1].
     * @param alphaMultiplier scaling factor for the alpha channel.
     */
    public GeneratorSmoke(int seed, float baseBrightness, float alphaMultiplier) {
        this.seed = seed;
        this.baseBrightness = baseBrightness;
        this.alphaMultiplier = alphaMultiplier;
    }

    @Override
    public Texture[] generate() {
        Layer smoke = SmokeProcedural.generate(seed, baseBrightness, alphaMultiplier);
        GLIntImage smoke_img = new GLIntImage(smoke);
        return new Texture[]{
                new Texture(smoke_img, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                        GL11.GL_LINEAR,
                        GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE)
        };
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof GeneratorSmoke other && seed == other.seed
                && Float.compare(baseBrightness, other.baseBrightness) == 0
                && Float.compare(alphaMultiplier, other.alphaMultiplier) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seed, baseBrightness, alphaMultiplier);
    }
}
