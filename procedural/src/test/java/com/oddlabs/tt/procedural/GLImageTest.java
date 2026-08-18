package com.oddlabs.tt.procedural;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GLImageTest {

    @Test
    void testGLIntImageCreationAndPixels() {
        GLIntImage image = new GLIntImage(64, 64, GL11.GL_RGBA);
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
        assertEquals(Integer.BYTES, image.getPixelSize());

        image.putPixel(10, 20, 0xFF112233);
        assertEquals(0xFF112233, image.getPixel(10, 20));

        GLImage scaled = image.scale(32, 32);
        assertEquals(32, scaled.getWidth());
        assertEquals(32, scaled.getHeight());
    }

    @Test
    void testGLByteImageCreationAndPixels() {
        GLByteImage image = new GLByteImage(16, 16, GL11.GL_RED);
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertEquals(Byte.BYTES, image.getPixelSize());

        image.putPixel(4, 5, 127);
        assertEquals(127, image.getPixel(4, 5));
    }
}
