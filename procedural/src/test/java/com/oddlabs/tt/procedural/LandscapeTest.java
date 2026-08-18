package com.oddlabs.tt.procedural;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.model.Terrain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LandscapeTest {

    @Test
    void testLandscapeGenerationNative() {
        IslandConfig config = new IslandConfig(
                Terrain.NATIVE,
                256,
                0.5f,
                0.5f,
                0.5f,
                42
        );

        Landscape landscape = new Landscape(2, config, 0.5f, 5, 0.5f);
        assertNotNull(landscape.getBlendInfos());
        assertTrue(landscape.getBlendInfos().length > 0);
        assertNotNull(landscape.getHeight());
        assertNotNull(landscape.getDetail());
        assertNotNull(landscape.getDetailNormal());
        assertEquals(256, landscape.getDetail().getWidth());
        assertEquals(256, landscape.getDetail().getHeight());
    }

    @Test
    void testLandscapeGenerationViking() {
        IslandConfig config = new IslandConfig(
                Terrain.VIKING,
                256,
                0.5f,
                0.5f,
                0.5f,
                123
        );

        Landscape landscape = new Landscape(2, config, 0.5f, 5, 0.5f);
        assertNotNull(landscape.getBlendInfos());
        assertTrue(landscape.getBlendInfos().length > 0);
        assertNotNull(landscape.getHeight());
        assertNotNull(Landscape.getDustColor(Terrain.VIKING));
        assertNotNull(Landscape.getDustColor(Terrain.NATIVE));
    }
}
