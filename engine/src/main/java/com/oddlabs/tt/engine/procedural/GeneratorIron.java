package com.oddlabs.tt.engine.procedural;

import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.NamedGenerator;
import com.oddlabs.tt.engine.resource.TextureGenerator;
import com.oddlabs.tt.procedural.image.IronProcedural;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

/**
 * Procedural texture generator for iron ore diffuse and normal textures.
 */
@NamedGenerator("Iron")
public final class GeneratorIron extends TextureGenerator {
    @Override
    public Texture[] generate() {
        IronProcedural.IronLayers layers = IronProcedural.generate();
        return new Texture[]{
                new Texture(new GLIntImage(layers.diffuse()), GL21.GL_SRGB8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                        GL11.GL_LINEAR,
                        GL11.GL_REPEAT, GL11.GL_REPEAT),
                new Texture(new GLIntImage(layers.normalMap()), GL11.GL_RGB, GL11.GL_LINEAR_MIPMAP_LINEAR,
                        GL11.GL_LINEAR,
                        GL11.GL_REPEAT, GL11.GL_REPEAT)
        };
    }
}
