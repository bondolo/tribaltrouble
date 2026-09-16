package com.oddlabs.tt.engine.procedural;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.TextureGenerator;
import com.oddlabs.tt.procedural.image.CrackProcedural;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Procedurally generates a Voronoi-based crack texture used for ground-impact decals.
 * The output is a single RGBA texture whose alpha encodes crack borders and whose
 * red/green channels provide an orange-tinted radial falloff.
 */
public final class GeneratorCrack extends TextureGenerator {
    @Override
    public Texture[] generate() {
        Layer layer = CrackProcedural.generate();
        return new Texture[]{
                new Texture(new GLIntImage(layer), GL11.GL_RGBA8,
                        GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE),
        };
    }
}
