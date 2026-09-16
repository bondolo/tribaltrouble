package com.oddlabs.tt.engine.procedural;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.TextureGenerator;
import com.oddlabs.tt.procedural.image.LightningProcedural;
import org.lwjgl.opengl.GL11;

/**
 * Procedural texture generator for lightning strike bolt effects.
 */
public final class GeneratorLightning extends TextureGenerator {
    @Override
    public Texture[] generate() {
        Layer layer = LightningProcedural.generate();
        GLIntImage img = new GLIntImage(layer);
        return new Texture[]{
                new Texture(img, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR,
                        GL11.GL_REPEAT, GL11.GL_REPEAT),
        };
    }
}
