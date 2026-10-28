package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.PlantLocation;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.SupplyPlacement;
import com.oddlabs.tt.simulation.landscape.TreePlacement;
import com.oddlabs.tt.simulation.model.Terrain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PathFinder} monotonic search identification and node state tracking.
 */
final class PathFinderTest {

    private record TestLandscape(float[] heightmap, int size) implements Landscape {
        @Override
        public IslandConfig config() {
            return new IslandConfig(Terrain.NATIVE, size, 0.5f, 0.5f, 0.5f, 42);
        }

        @Override
        public Terrain terrain() {
            return Terrain.NATIVE;
        }

        @Override
        public float seaLevelMeters() {
            return 0f;
        }

        @Override
        public List<TreePlacement> trees() {
            return List.of();
        }

        @Override
        public List<TreePlacement> palmTrees() {
            return List.of();
        }

        @Override
        public List<SupplyPlacement> rocks() {
            return List.of();
        }

        @Override
        public List<SupplyPlacement> iron() {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<PlantLocation>[] plants() {
            return new ArrayList[0];
        }

        @Override
        public boolean[][] accessGrid() {
            return new boolean[size][size];
        }

        @Override
        public byte[][] buildGrid() {
            return new byte[0][0];
        }

        @Override
        public List<List<SpawnLocation>> startingLocations() {
            return List.of();
        }
    }

    private UnitGrid unitGrid;
    private PathFinder pathFinder;

    @BeforeEach
    void setUp() {
        int size = 128;
        Landscape landscape = new TestLandscape(new float[size * size], size * HeightMap.METERS_PER_UNIT_GRID);
        HeightMap heightMap = new HeightMap(null, landscape);
        unitGrid = new UnitGrid(heightMap);
        pathFinder = unitGrid.pathFinder();
    }

    @Test
    void testMonotonicSearchIdIncrements() {
        int initialSearchId = pathFinder.getSearchId();

        Region r1 = new Region();
        r1.setPosition(10, 10);
        Region r2 = new Region();
        r2.setPosition(20, 20);
        Region.link(r1, r2);
        unitGrid.setRegion(10, 10, r1);
        unitGrid.setRegion(20, 20, r2);

        Optional<Region> path1 = pathFinder.findPathRegion(r1, r2);
        assertTrue(path1.isPresent());
        int firstSearchId = pathFinder.getSearchId();
        assertTrue(firstSearchId > initialSearchId);

        Optional<Region> path2 = pathFinder.findPathRegion(r1, r2);
        assertTrue(path2.isPresent());
        int secondSearchId = pathFinder.getSearchId();
        assertEquals(firstSearchId + 1, secondSearchId);
    }

    @Test
    void testNodeVisitedStatePerSearchId() {
        Region r1 = new Region();
        r1.setPosition(5, 5);
        Region r2 = new Region();
        r2.setPosition(15, 15);
        Region.link(r1, r2);
        unitGrid.setRegion(5, 5, r1);
        unitGrid.setRegion(15, 15, r2);

        pathFinder.findPathRegion(r1, r2);
        int searchId1 = pathFinder.getSearchId();

        assertTrue(r1.isVisited(searchId1));
        assertTrue(r2.isVisited(searchId1));
        assertFalse(r1.isVisited(searchId1 + 1));
        assertFalse(r2.isVisited(searchId1 + 1));
    }

    @Test
    void testGridNodeSearchIdTracking() {
        GridNode.Offset offset = new GridNode.Offset(0, 0);
        GridNode node = pathFinder.getPathfinderNode(offset, 10, 10);
        assertNotNull(node);

        // An unsearched node has search_id = 0, so it is not visited for active positive search IDs.
        assertFalse(node.isVisited(1));

        node.setPathInitial(1, 100);
        assertTrue(node.isVisited(1));
        assertFalse(node.isVisited(2));

        node.resetSearchId();
        assertFalse(node.isVisited(1));
    }
}
