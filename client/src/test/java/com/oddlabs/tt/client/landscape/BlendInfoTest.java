package com.oddlabs.tt.client.landscape;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class BlendInfoTest {

    @Test
    void testStructureBlend() {
        Layer structure = new Layer(16, 16);
        Layer normal = new Layer(16, 16);
        Channel alpha = new Channel(16, 16).fill(0.5f);

        StructureBlend blend = new StructureBlend(structure, normal, alpha, 1.0f);
        assertSame(structure, blend.structureLayer());
        assertSame(normal, blend.normalLayer());
        assertSame(alpha, blend.alphaChannel());
        assertEquals(1.0f, blend.alphaPower());

        StructureBlend customBlend = new StructureBlend(structure, normal, alpha, 0.85f);
        assertEquals(0.85f, customBlend.alphaPower());
    }

    @Test
    void testBlendLighting() {
        Channel alpha = new Channel(8, 8).fill(1.0f);
        Color color = new Color.Standard(0xFF_FF_E6_99);

        BlendLighting blend = new BlendLighting(alpha, color);
        assertSame(alpha, blend.alphaChannel());
        assertNotNull(blend.color());
        assertEquals(new Color.Linear(color).r(), blend.color().r(), 0.001f);
    }

    @Test
    void testBlendOcclusion() {
        Channel alpha = new Channel(8, 8).fill(0.8f);
        Color color = new Color.Standard(0xFF_55_4C_66);

        BlendOcclusion blend = new BlendOcclusion(alpha, color);
        assertSame(alpha, blend.alphaChannel());
        assertNotNull(blend.color());
        assertEquals(new Color.Linear(color).r(), blend.color().r(), 0.001f);
    }
}
