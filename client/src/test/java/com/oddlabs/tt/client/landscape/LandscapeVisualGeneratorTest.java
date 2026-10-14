package com.oddlabs.tt.client.landscape;

import com.oddlabs.tt.procedural.landscape.ProceduralLandscape;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.model.Terrain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests procedural landscape visual texture generation, normal maps, and blend layer assemblies.
 */
final class LandscapeVisualGeneratorTest {

    @Test
    void testLandscapeVisualGenerationNative() {
        IslandConfig config = new IslandConfig(
                Terrain.NATIVE,
                256,
                0.5f,
                0.5f,
                0.5f,
                42
        );

        ProceduralLandscape landscape = new ProceduralLandscape(2, config, 5, 0.5f);
        assertNotNull(landscape.heightmap());

        LandscapeBakingData bakingData = LandscapeVisualGenerator.generate(
                landscape,
                0.15f,
                0.70f
        );
        assertNotNull(bakingData);
        assertNotNull(bakingData.blendInfos());
        assertTrue(bakingData.blendInfos().length > 0);
        assertNotNull(bakingData.detail());
        assertNotNull(bakingData.detailNormal());
        assertEquals(256, bakingData.detail().getWidth());
        assertEquals(256, bakingData.detail().getHeight());
    }

    @Test
    void testLandscapeVisualGenerationViking() {
        IslandConfig config = new IslandConfig(
                Terrain.VIKING,
                256,
                0.5f,
                0.5f,
                0.5f,
                123
        );

        ProceduralLandscape landscape = new ProceduralLandscape(2, config, 5, 0.5f);
        assertNotNull(landscape.heightmap());

        LandscapeBakingData bakingData = LandscapeVisualGenerator.generate(
                landscape,
                0.15f,
                0.70f
        );
        assertNotNull(bakingData);
        assertNotNull(bakingData.blendInfos());
        assertTrue(bakingData.blendInfos().length > 0);
    }

    @Test
    void testLandscapeVisualGenerationVikingIsland1() {
        IslandConfig config = new IslandConfig(
                Terrain.VIKING,
                256,
                0.75f,
                1.0f,
                0.5f,
                1
        );
        ProceduralLandscape landscape = new ProceduralLandscape(1, config, 5, 0.5f);
        LandscapeBakingData bakingData = LandscapeVisualGenerator.generate(
                landscape,
                0.15f,
                0.70f
        );
        assertNotNull(bakingData);
        BlendInfo[] blendInfos = bakingData.blendInfos();
        assertEquals(8, blendInfos.length);
        assertTrue(blendInfos[3] instanceof StructureBlend);
        StructureBlend grassBlend = (StructureBlend) blendInfos[3];
        assertEquals(0.70f, grassBlend.alphaPower(), 0.001f);
    }
}
