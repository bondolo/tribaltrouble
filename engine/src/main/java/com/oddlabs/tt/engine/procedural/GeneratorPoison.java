package com.oddlabs.tt.engine.procedural;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.TextureGenerator;
import com.oddlabs.tt.procedural.image.PoisonProcedural;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Procedural texture generator for poison magic bubble effects.
 */
public final class GeneratorPoison extends TextureGenerator {
    @Override
    public Texture[] generate() {
        Layer poison = PoisonProcedural.generate();
        GLIntImage poison_img = new GLIntImage(poison);
        return new Texture[]{
                new Texture(poison_img, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                        GL11.GL_LINEAR,
                        GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE)
        };
    }
}
