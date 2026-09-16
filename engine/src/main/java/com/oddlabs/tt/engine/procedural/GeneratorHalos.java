package com.oddlabs.tt.engine.procedural;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.TextureGenerator;
import com.oddlabs.tt.procedural.image.HalosProcedural;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.Arrays;
import java.util.Map;

/**
 * Procedural texture generator for selection and shadow halos.
 */
public final class GeneratorHalos extends TextureGenerator {
    /**
     * Types of halo textures that can be generated.
     */
    public enum HaloType {
        SHADOWED,
        SELECTED
    }

    private final int size;
    private final float[][] shadow_parms;
    private final float[][] ring_parms;

    public GeneratorHalos(int size, float[][] shadow_parms,
            float[][] ring_parms) {
        this.size = size;
        this.shadow_parms = shadow_parms;
        this.ring_parms = ring_parms;
    }

    @Override
    public Texture[] generate() {
        Map<HalosProcedural.HaloType, Layer> layers = HalosProcedural.generate(size, shadow_parms, ring_parms);
        Texture[] textures = new Texture[HaloType.values().length];
        for (HaloType type : HaloType.values()) {
            HalosProcedural.HaloType procType = HalosProcedural.HaloType.valueOf(type.name());
            Layer layer = layers.get(procType);
            int idx = type.ordinal();
            textures[idx] = new Texture(new GLImage[]{new GLIntImage(layer)}, GL11.GL_RGBA8,
                    GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
        }
        return textures;
    }

    @Override
    public int hashCode() {
        return size * Arrays.deepHashCode(shadow_parms) * Arrays.deepHashCode(ring_parms);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return super.equals(o) &&
                o instanceof GeneratorHalos other &&
                size == other.size &&
                Arrays.deepEquals(shadow_parms, other.shadow_parms) &&
                Arrays.deepEquals(ring_parms, other.ring_parms);
    }
}
