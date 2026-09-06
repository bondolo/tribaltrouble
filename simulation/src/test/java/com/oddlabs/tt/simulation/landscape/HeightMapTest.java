package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Terrain;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests terrain height calculation and planar triangle interpolation on {@link HeightMap}.
 */
final class HeightMapTest {

    private record TestLandscapeData(float[] heightmap, int metersPerWorld) implements LandscapeData {
        @Override
        public Terrain terrain() {
            return Terrain.NATIVE;
        }

        @Override
        public float seaLevelMeters() {
            return 0f;
        }

        @Override
        public List<int[]> trees() {
            return List.of();
        }

        @Override
        public List<int[]> palmTrees() {
            return List.of();
        }

        @Override
        public List<int[]> rocks() {
            return List.of();
        }

        @Override
        public List<int[]> iron() {
            return List.of();
        }

        @Override
        public float[][] plants() {
            return new float[0][0];
        }

        @Override
        public boolean[][] accessGrid() {
            return new boolean[0][0];
        }

        @Override
        public byte[][] buildGrid() {
            return new byte[0][0];
        }

        @Override
        public float[][] startingLocations() {
            return new float[0][0];
        }
    }

    @Test
    void testPlanarTriangleInterpolation() {
        // Grid of 16x16 units (1 patch of 16 units) = 256 vertices
        int size = 16;
        float[] heightmap = new float[size * size];

        // Define a quad at (x=0, y=0) with corner heights:
        // h00 (0,0) = 0
        // h10 (1,0) = 10
        // h01 (0,1) = 20
        // h11 (1,1) = 50
        heightmap[0] = 0f;          // (0,0)
        heightmap[1] = 10f;         // (1,0)
        heightmap[size] = 20f;      // (0,1)
        heightmap[size + 1] = 50f;  // (1,1)

        LandscapeData data = new TestLandscapeData(heightmap, size * HeightMap.METERS_PER_UNIT_GRID);
        HeightMap map = new HeightMap(null, data);

        // Grid unit is 2 meters.
        // Vertex (0,0) is at world pos (0m, 0m)
        assertEquals(0f, map.computeInterpolatedHeight(0, 0f, 0f), 1e-5f);
        // Vertex (1,0) is at world pos (2m, 0m)
        assertEquals(10f, map.computeInterpolatedHeight(0, 2f, 0f), 1e-5f);
        // Vertex (0,1) is at world pos (0m, 2m)
        assertEquals(20f, map.computeInterpolatedHeight(0, 0f, 2f), 1e-5f);
        // Vertex (1,1) is at world pos (2m, 2m)
        assertEquals(50f, map.computeInterpolatedHeight(0, 2f, 2f), 1e-5f);

        // Point in Triangle 1: (x=0.5m, y=0.5m) -> dx = 0.25, dy = 0.25 (dx + dy = 0.5 < 1.0)
        // h = h00 + dx*(h10 - h00) + dy*(h01 - h00) = 0 + 0.25*10 + 0.25*20 = 7.5
        assertEquals(7.5f, map.computeInterpolatedHeight(0, 0.5f, 0.5f), 1e-5f);

        // Point in Triangle 2: (x=1.5m, y=1.5m) -> dx = 0.75, dy = 0.75 (dx + dy = 1.5 >= 1.0)
        // h = h11 + (1-dx)*(h01 - h11) + (1-dy)*(h10 - h11) = 50 + 0.25*(20 - 50) + 0.25*(10 - 50)
        //   = 50 - 7.5 - 10 = 32.5
        assertEquals(32.5f, map.computeInterpolatedHeight(0, 1.5f, 1.5f), 1e-5f);

        // Point on Diagonal: (x=1.0m, y=1.0m) -> dx = 0.5, dy = 0.5 (dx + dy = 1.0)
        // Triangle 1: 0 + 0.5*10 + 0.5*20 = 15.0
        // Triangle 2: 50 + 0.5*(20 - 50) + 0.5*(10 - 50) = 50 - 15 - 20 = 15.0
        // Both triangles must yield exactly 15.0 along the diagonal
        assertEquals(15.0f, map.computeInterpolatedHeight(0, 1.0f, 1.0f), 1e-5f);
    }
}
