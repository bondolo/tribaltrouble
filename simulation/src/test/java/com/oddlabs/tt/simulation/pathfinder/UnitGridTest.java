package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;
import com.oddlabs.tt.simulation.landscape.LandscapeGeometry;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.PlantLocation;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.SupplyPlacement;
import com.oddlabs.tt.simulation.landscape.TreePlacement;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests spatial grid target discovery and parity on {@link UnitGrid}.
 */
class UnitGridTest {

    private record TestLandscape(int size,
                                 float[] heightmap,
                                 boolean[][] accessGrid,
                                 byte[][] buildGrid,
                                 List<List<SpawnLocation>> startingLocations) implements Landscape {

        static TestLandscape create(int size) {
            float[] heightmap = new float[size * size];
            Arrays.fill(heightmap, 5.0f);

            boolean[][] accessGrid = new boolean[size][size];
            for (boolean[] row : accessGrid) {
                Arrays.fill(row, true);
            }

            byte[][] buildGrid = new byte[size][size];
            for (byte[] row : buildGrid) {
                Arrays.fill(row, (byte) 20);
            }

            List<List<SpawnLocation>> startingLocations = List.of(
                    List.of(new SpawnLocation(10.0f, 10.0f)),
                    List.of(new SpawnLocation(20.0f, 20.0f))
            );

            return new TestLandscape(size, heightmap, accessGrid, buildGrid, startingLocations);
        }

        @Override
        public IslandConfig config() {
            return new IslandConfig(Terrain.NATIVE, metersPerWorld(), 0.5f, 0.5f, 0.5f, 42);
        }

        @Override
        public Terrain terrain() {
            return Terrain.NATIVE;
        }

        @Override
        public int metersPerWorld() {
            return size * HeightMap.METERS_PER_UNIT_GRID;
        }

        @Override
        public float seaLevelMeters() {
            return 0.0f;
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
    }

    private UnitGrid unitGrid;

    @BeforeEach
    void setUp() {
        int gridSize = 32;
        Landscape landscape = TestLandscape.create(gridSize);
        WorldParameters worldParams = new WorldParameters(0, "1", 5, 20);
        List<PlayerInfo> playerInfos = List.of(
                new PlayerInfo(0, Race.VIKINGS, "Viking"),
                new PlayerInfo(1, Race.NATIVES, "Native")
        );
        Color.Linear[] playerColors = new Color.Linear[]{
                new Color.Linear(1f, 0f, 0f, 1f),
                new Color.Linear(0f, 0f, 1f, 1f)
        };
        LandscapeBoundsProvider boundsProvider = new LandscapeGeometry();
        RaceData raceData = new RaceData();
        NotificationListener noopListener = new NotificationListener() {
        };

        World world = World.newWorld(
                raceData,
                boundsProvider,
                noopListener,
                worldParams,
                landscape,
                playerInfos,
                playerColors,
                false
        );
        unitGrid = world.getUnitGrid();
    }

    @Test
    void testFindGridTargetMatchesCenterWhenUnoccupied() {
        Target target = unitGrid.findGridTarget(10, 10, false);
        assertNotNull(target);
        assertEquals(10, target.getGridX());
        assertEquals(10, target.getGridY());
    }

    @Test
    void testFindGridTargetWithGridTargetsOnlyRespectsCheckerboard() {
        Target targetOdd = unitGrid.findGridTarget(10, 11, true);
        assertNotNull(targetOdd);
        assertEquals(0, (targetOdd.getGridX() + targetOdd.getGridY()) & 1,
                "Checkerboard grid target coordinates must sum to an even number");

        Target targetEven = unitGrid.findGridTarget(10, 10, true);
        assertNotNull(targetEven);
        assertEquals(10, targetEven.getGridX());
        assertEquals(10, targetEven.getGridY());
    }

    @Test
    void testFindGridTargetMatchesFindGridTargetsOutput() {
        for (int x = 5; x <= 25; x += 5) {
            for (int y = 5; y <= 25; y += 5) {
                Target singleTarget = unitGrid.findGridTarget(x, y, false);
                Target[] arrayTarget = unitGrid.findGridTargets(x, y, 1, false);

                assertNotNull(singleTarget);
                assertNotNull(arrayTarget);
                assertEquals(1, arrayTarget.length);
                assertNotNull(arrayTarget[0]);
                assertEquals(arrayTarget[0].getGridX(), singleTarget.getGridX());
                assertEquals(arrayTarget[0].getGridY(), singleTarget.getGridY());
            }
        }
    }

    @Test
    void testChunkOccupancyTracking() {
        int chunkX = 2;
        int chunkY = 2;
        // Chunk (2, 2) covers grid coordinates [16, 23] in X and Y
        org.junit.jupiter.api.Assertions.assertFalse(unitGrid.isChunkOccupied(chunkX, chunkY));

        StaticOccupant occ1 = new StaticOccupant();
        StaticOccupant occ2 = new StaticOccupant();

        unitGrid.occupyGrid(16, 16, occ1);
        org.junit.jupiter.api.Assertions.assertTrue(unitGrid.isChunkOccupied(chunkX, chunkY));

        unitGrid.occupyGrid(17, 18, occ2);
        org.junit.jupiter.api.Assertions.assertTrue(unitGrid.isChunkOccupied(chunkX, chunkY));

        unitGrid.freeGrid(16, 16, occ1);
        org.junit.jupiter.api.Assertions.assertTrue(unitGrid.isChunkOccupied(chunkX, chunkY));

        unitGrid.freeGrid(17, 18, occ2);
        org.junit.jupiter.api.Assertions.assertFalse(unitGrid.isChunkOccupied(chunkX, chunkY));
    }

    @Test
    void testScanOccupantsExactParityWithScanAll() {
        StaticOccupant occA = new StaticOccupant();
        StaticOccupant occB = new StaticOccupant();
        StaticOccupant occC = new StaticOccupant();

        unitGrid.occupyGrid(12, 10, occA);
        unitGrid.occupyGrid(18, 14, occB);
        unitGrid.occupyGrid(10, 18, occC);

        List<Occupant> visitedWithScanAll = new ArrayList<>();
        List<Occupant> visitedWithScanOccupants = new ArrayList<>();

        ScanFilter allFilter = new ScanFilter() {
            @Override
            public int getMinRadius() {
                return 0;
            }

            @Override
            public int getMaxRadius() {
                return 20;
            }

            @Override
            public boolean requiresOccupant() {
                return false;
            }

            @Override
            public boolean filter(int grid_x, int grid_y, @Nullable Occupant occ) {
                if (occ != null) {
                    visitedWithScanAll.add(occ);
                }
                return false;
            }
        };

        ScanFilter occupantsFilter = new ScanFilter() {
            @Override
            public int getMinRadius() {
                return 0;
            }

            @Override
            public int getMaxRadius() {
                return 20;
            }

            @Override
            public boolean requiresOccupant() {
                return true;
            }

            @Override
            public boolean filter(int grid_x, int grid_y, @Nullable Occupant occ) {
                if (occ != null) {
                    visitedWithScanOccupants.add(occ);
                }
                return false;
            }
        };

        unitGrid.scan(allFilter, 15, 15);
        unitGrid.scan(occupantsFilter, 15, 15);

        assertEquals(3, visitedWithScanAll.size());
        assertEquals(visitedWithScanAll, visitedWithScanOccupants);

        unitGrid.freeGrid(12, 10, occA);
        unitGrid.freeGrid(18, 14, occB);
        unitGrid.freeGrid(10, 18, occC);
    }

    @Test
    void test1DRegionsAndOccupantsConsistency() {
        int gx = 7;
        int gy = 19;
        StaticOccupant occupant = new StaticOccupant();
        org.junit.jupiter.api.Assertions.assertFalse(unitGrid.isGridOccupied(gx, gy));
        org.junit.jupiter.api.Assertions.assertNull(unitGrid.getOccupant(gx, gy));

        unitGrid.occupyGrid(gx, gy, occupant);
        org.junit.jupiter.api.Assertions.assertTrue(unitGrid.isGridOccupied(gx, gy));
        assertEquals(occupant, unitGrid.getOccupant(gx, gy));

        unitGrid.freeGrid(gx, gy, occupant);
        org.junit.jupiter.api.Assertions.assertFalse(unitGrid.isGridOccupied(gx, gy));
        org.junit.jupiter.api.Assertions.assertNull(unitGrid.getOccupant(gx, gy));
    }
}
