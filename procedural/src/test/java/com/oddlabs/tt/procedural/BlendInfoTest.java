package com.oddlabs.tt.procedural;

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
        GLIntImage structure = new GLIntImage(new Layer(16, 16));
        GLIntImage normal = new GLIntImage(new Layer(16, 16));
        GLByteImage alpha = new GLByteImage(new Channel(16, 16).fill(0.5f));

        StructureBlend blend = new StructureBlend(structure, normal, alpha);
        assertSame(structure, blend.getStructureImage());
        assertSame(normal, blend.getNormalImage());
        assertSame(alpha, blend.getSourceImage());
    }

    @Test
    void testBlendLighting() {
        GLByteImage alpha = new GLByteImage(new Channel(8, 8).fill(1.0f));
        Color color = new Color.Standard(0xFF_FF_E6_99);

        BlendLighting blend = new BlendLighting(alpha, color);
        assertSame(alpha, blend.getSourceImage());
        assertNotNull(blend.getColor());
        assertEquals(new Color.Linear(color).r(), blend.getColor().r(), 0.001f);
    }

    @Test
    void testBlendOcclusion() {
        GLByteImage alpha = new GLByteImage(new Channel(8, 8).fill(0.8f));
        Color color = new Color.Standard(0xFF_55_4C_66);

        BlendOcclusion blend = new BlendOcclusion(alpha, color);
        assertSame(alpha, blend.getSourceImage());
        assertNotNull(blend.getColor());
        assertEquals(new Color.Linear(color).r(), blend.getColor().r(), 0.001f);
    }
}
