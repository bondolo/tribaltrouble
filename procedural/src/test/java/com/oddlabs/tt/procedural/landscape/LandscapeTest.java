package com.oddlabs.tt.procedural.landscape;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.model.Terrain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests procedural landscape generation, resource distribution, and terrain blending.
 */
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

        ProceduralLandscape landscape = new ProceduralLandscape(2, config, 5, 0.5f);
        assertNotNull(landscape.heightmap());
        assertTrue(landscape.heightmap().length > 0);
        assertNotNull(landscape.trees());
        assertNotNull(landscape.palmTrees());
        assertNotNull(landscape.rocks());
        assertNotNull(landscape.iron());
        assertNotNull(landscape.startingLocations());
        assertEquals(2, landscape.startingLocations().size());
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

        ProceduralLandscape landscape = new ProceduralLandscape(2, config, 5, 0.5f);
        assertNotNull(landscape.heightmap());
        assertTrue(landscape.heightmap().length > 0);
        assertNotNull(landscape.trees());
        assertNotNull(landscape.palmTrees());
        assertNotNull(landscape.rocks());
        assertNotNull(landscape.iron());
        assertNotNull(Terrain.VIKING.getDustColor());
        assertNotNull(Terrain.NATIVE.getDustColor());
        assertNotNull(landscape.startingLocations());
        assertEquals(2, landscape.startingLocations().size());
    }
}
