package com.oddlabs.tt.engine.procedural;

import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.TextureGenerator;
import org.lwjgl.opengl.GL11;

public final class GeneratorRespond extends TextureGenerator {
    private static final int COLOR = 0x80808080;

    @Override
    public Texture[] generate() {
        GLIntImage img = new GLIntImage(1, 1, GL11.GL_RGBA);
        img.putPixel(0, 0, COLOR);
        Texture[] textures = new Texture[1];
        textures[0] = new Texture(new GLImage[]{img}, RenderConfig.COMPRESSED_RGBA_FORMAT, GL11.GL_NEAREST,
                GL11.GL_NEAREST,
                GL11.GL_REPEAT, GL11.GL_REPEAT);
        return textures;
    }

    @Override
    public int hashCode() {
        return 1234;
    }
}
