package com.oddlabs.tt.engine.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class IconQuadTest {

    @Test
    void testValidBoundsAndCoords() {
        IconQuad quad = new IconQuad(0.0f, 0.0f, 0.5f, 0.5f, 64, 64, new DummyTexture());
        assertEquals(0.0f, quad.getU1(), 0.0001f);
        assertEquals(0.0f, quad.getV1(), 0.0001f);
        assertEquals(0.5f, quad.getU2(), 0.0001f);
        assertEquals(0.5f, quad.getV2(), 0.0001f);
        assertEquals(64, quad.getWidth());
        assertEquals(64, quad.getHeight());
    }

    @Test
    void testInvalidTextureCoordinatesThrow() {
        assertThrows(IllegalArgumentException.class, () -> new IconQuad(0.6f, 0.0f, 0.4f, 0.5f, 64, 64,
                new DummyTexture()));
        assertThrows(IllegalArgumentException.class, () -> new IconQuad(0.0f, 0.8f, 0.5f, 0.2f, 64, 64,
                new DummyTexture()));
        assertThrows(IllegalArgumentException.class, () -> new IconQuad(Float.NaN, 0.0f, 0.5f, 0.5f, 64, 64,
                new DummyTexture()));
        assertThrows(IllegalArgumentException.class, () -> new IconQuad(0.0f, 0.0f, 0.5f, 0.5f, -10, 64,
                new DummyTexture()));
    }

    @Test
    void testModeIconQuads() {
        DummyTexture texture = new DummyTexture();
        IconQuad normal = new IconQuad(0.0f, 0.0f, 0.2f, 0.2f, 32, 32, texture);
        IconQuad active = new IconQuad(0.2f, 0.0f, 0.4f, 0.2f, 32, 32, texture);
        IconQuad disabled = new IconQuad(0.4f, 0.0f, 0.6f, 0.2f, 32, 32, texture);

        ModeIconQuads modeQuads = new ModeIconQuads(normal, active, disabled);
        assertEquals(normal, modeQuads.quad(ModeIconQuads.Mode.NORMAL));
        assertEquals(active, modeQuads.quad(ModeIconQuads.Mode.ACTIVE));
        assertEquals(disabled, modeQuads.quad(ModeIconQuads.Mode.DISABLED));
    }

    private static final class DummyTexture extends Texture {
        DummyTexture() {
            super(128, 128);
        }
    }
}
