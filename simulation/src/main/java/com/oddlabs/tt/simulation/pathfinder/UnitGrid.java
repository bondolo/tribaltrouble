package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Spatial grid partitioning the game world into regions, occupants, and height coordinates.
 */
public final class UnitGrid {
    private static final int CHUNK_SHIFT = 3;
    private static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;

    private final HeightMap heightmap;
    private final int gridSize;
    private final @Nullable Region[] regions;
    private final @Nullable Occupant[] occupants;
    private final int chunksAcross;
    private final int[] chunkOccupantCounts;
    private final long[] chunkBitmask;
    private int totalOccupants = 0;
    private final PathFinder pathFinder = new PathFinder(this);

    public UnitGrid(HeightMap heightmap) {
        this.heightmap = heightmap;
        this.gridSize = heightmap.getAccessGrid().length;
        this.occupants = new Occupant[gridSize * gridSize];
        this.regions = new Region[gridSize * gridSize];
        this.chunksAcross = (gridSize + CHUNK_SIZE - 1) >>> CHUNK_SHIFT;
        int totalChunks = chunksAcross * chunksAcross;
        this.chunkOccupantCounts = new int[totalChunks];
        this.chunkBitmask = new long[(totalChunks + 63) >>> 6];
    }

    boolean isChunkOccupied(int chunkX, int chunkY) {
        if (chunkX < 0 || chunkX >= chunksAcross || chunkY < 0 || chunkY >= chunksAcross) {
            return false;
        }
        int chunkIndex = chunkY * chunksAcross + chunkX;
        return (chunkBitmask[chunkIndex >>> 6] & (1L << (chunkIndex & 63))) != 0;
    }

    private boolean filter(ScanFilter filter, int x, int y) {
        return x >= 0 && y >= 0 && x < gridSize && y < gridSize && filter.filter(x, y, occupants[y * gridSize + x]);
    }

    private boolean filterOccupant(ScanFilter filter, int x, int y) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return false;
        }
        Occupant occ = occupants[y * gridSize + x];
        return occ != null && filter.filter(x, y, occ);
    }

    /**
     * Finds a single open landscape target without allocating filter or array objects.
     *
     * @param center_grid_x the center x coordinate
     * @param center_grid_y the center y coordinate
     * @param grid_targets_only whether to restrict to checkerboard grid coordinates ((x + y) & 1 == 0)
     * @return the nearest valid Target, or null if none found
     */
    public @Nullable Target findGridTarget(int center_grid_x, int center_grid_y, boolean grid_targets_only) {
        if (isValidTarget(center_grid_x, center_grid_y, grid_targets_only)) {
            return new LandscapeTarget(center_grid_x, center_grid_y);
        }
        int radius = 1;
        int max_radius = gridSize;
        while (radius <= max_radius) {
            int x = center_grid_x - radius;
            int x2 = center_grid_x + radius;
            int y = center_grid_y - radius;
            int y2 = center_grid_y + radius;
            if (x < 0 && x2 >= gridSize && y < 0 && y2 >= gridSize) {
                break;
            }
            for (int i = 0; i < 2 * radius - 1; i++) {
                int y_i = center_grid_y - radius + 1 + i;
                if (isValidTarget(x, y_i, grid_targets_only)) {
                    return new LandscapeTarget(x, y_i);
                }
                if (isValidTarget(x2, y_i, grid_targets_only)) {
                    return new LandscapeTarget(x2, y_i);
                }
            }
            for (int i = 0; i < 2 * radius + 1; i++) {
                int x_i = center_grid_x - radius + i;
                if (isValidTarget(x_i, y, grid_targets_only)) {
                    return new LandscapeTarget(x_i, y);
                }
                if (isValidTarget(x_i, y2, grid_targets_only)) {
                    return new LandscapeTarget(x_i, y2);
                }
            }
            radius++;
        }
        return null;
    }

    private boolean isValidTarget(int x, int y, boolean grid_targets_only) {
        return x >= 0 && y >= 0 && x < gridSize && y < gridSize
                && (!grid_targets_only || ((x + y) & 1) == 0)
                && occupants[y * gridSize + x] == null;
    }

    public @Nullable Target[] findGridTargets(int center_grid_x, int center_grid_y, int num_targets,
            boolean grid_targets_only) {
        if (num_targets == 1) {
            Target target = findGridTarget(center_grid_x, center_grid_y, grid_targets_only);
            return new Target[]{target};
        }
        FindTargetsFilter filter = new FindTargetsFilter(num_targets, gridSize, grid_targets_only);
        scan(filter, center_grid_x, center_grid_y);
        return filter.getTargets();
    }

    public void scan(ScanFilter filter, int center_grid_x, int center_grid_y) {
        if (filter.requiresOccupant()) {
            scanOccupants(filter, center_grid_x, center_grid_y);
        } else {
            scanAll(filter, center_grid_x, center_grid_y);
        }
    }

    private void scanAll(ScanFilter filter, int center_grid_x, int center_grid_y) {
        int radius = filter.getMinRadius();
        if (radius == 0) {
            if (filter(filter, center_grid_x, center_grid_y)) {
                return;
            }
            radius++;
        }
        int max_radius = filter.getMaxRadius();
        while (radius <= max_radius) {
            int x = center_grid_x - radius;
            int x2 = center_grid_x + radius;
            int y = center_grid_y - radius;
            int y2 = center_grid_y + radius;
            if (x < 0 && x2 >= gridSize && y < 0 && y2 >= gridSize) {
                break;
            }
            for (int i = 0; i < 2 * radius - 1; i++) {
                int y_i = center_grid_y - radius + 1 + i;
                if (filter(filter, x, y_i) || filter(filter, x2, y_i)) {
                    return;
                }
            }
            for (int i = 0; i < 2 * radius + 1; i++) {
                int x_i = center_grid_x - radius + i;
                if (filter(filter, x_i, y) || filter(filter, x_i, y2)) {
                    return;
                }
            }
            radius++;
        }
    }

    private void scanOccupants(ScanFilter filter, int center_grid_x, int center_grid_y) {
        if (totalOccupants == 0) {
            return;
        }
        int radius = filter.getMinRadius();
        if (radius == 0) {
            if (filterOccupant(filter, center_grid_x, center_grid_y)) {
                return;
            }
            radius++;
        }
        int max_radius = filter.getMaxRadius();
        while (radius <= max_radius) {
            int x = center_grid_x - radius;
            int x2 = center_grid_x + radius;
            int y = center_grid_y - radius;
            int y2 = center_grid_y + radius;
            if (x < 0 && x2 >= gridSize && y < 0 && y2 >= gridSize) {
                break;
            }
            int y_start = center_grid_y - radius + 1;
            int y_end = center_grid_y + radius - 1;
            int y_i = y_start;
            int chunkX1 = x >> CHUNK_SHIFT;
            int chunkX2 = x2 >> CHUNK_SHIFT;
            while (y_i <= y_end) {
                int chunkY = y_i >> CHUNK_SHIFT;
                int chunkMaxY = ((chunkY + 1) << CHUNK_SHIFT) - 1;
                int stepLimit = Math.min(y_end, chunkMaxY);

                boolean occ1 = isChunkOccupied(chunkX1, chunkY);
                boolean occ2 = isChunkOccupied(chunkX2, chunkY);

                if (!occ1 && !occ2) {
                    y_i = stepLimit + 1;
                } else {
                    while (y_i <= stepLimit) {
                        if (occ1 && filterOccupant(filter, x, y_i)) {
                            return;
                        }
                        if (occ2 && filterOccupant(filter, x2, y_i)) {
                            return;
                        }
                        y_i++;
                    }
                }
            }

            int x_start = center_grid_x - radius;
            int x_end = center_grid_x + radius;
            int x_i = x_start;
            int chunkY1 = y >> CHUNK_SHIFT;
            int chunkY2 = y2 >> CHUNK_SHIFT;
            while (x_i <= x_end) {
                int chunkX = x_i >> CHUNK_SHIFT;
                int chunkMaxX = ((chunkX + 1) << CHUNK_SHIFT) - 1;
                int stepLimit = Math.min(x_end, chunkMaxX);

                boolean occ1 = isChunkOccupied(chunkX, chunkY1);
                boolean occ2 = isChunkOccupied(chunkX, chunkY2);

                if (!occ1 && !occ2) {
                    x_i = stepLimit + 1;
                } else {
                    while (x_i <= stepLimit) {
                        if (occ1 && filterOccupant(filter, x_i, y)) {
                            return;
                        }
                        if (occ2 && filterOccupant(filter, x_i, y2)) {
                            return;
                        }
                        x_i++;
                    }
                }
            }
            radius++;
        }
    }

    public static float coordinateFromGrid(int g) {
        return (g + .5f) * HeightMap.METERS_PER_UNIT_GRID;
    }

    public static int toGridCoordinate(float c) {
        return (int) (c / HeightMap.METERS_PER_UNIT_GRID);
    }

    public int getGridSize() {
        return gridSize;
    }

    public @Nullable Region getRegion(int grid_x, int grid_y) {
        return regions[grid_y * gridSize + grid_x];
    }

    public void setRegion(int grid_x, int grid_y, Region r) {
        int index = grid_y * gridSize + grid_x;
        assert regions[index] == null && occupants[index] == null;
        regions[index] = r;
    }

    public boolean isGridOccupied(int grid_x, int grid_y) {
        return occupants[grid_y * gridSize + grid_x] != null;
    }

    public @Nullable Occupant getOccupant(int grid_x, int grid_y) {
        return occupants[grid_y * gridSize + grid_x];
    }

    public void occupyGrid(int grid_x, int grid_y, Occupant occupant) {
        int index = grid_y * gridSize + grid_x;
        assert occupants[index] == null : "Grid (" + grid_x + ", " + grid_y + ") already occupied by "
                + occupants[index];
        occupants[index] = occupant;
        totalOccupants++;
        int chunkIndex = (grid_y >>> CHUNK_SHIFT) * chunksAcross + (grid_x >>> CHUNK_SHIFT);
        if (chunkOccupantCounts[chunkIndex]++ == 0) {
            chunkBitmask[chunkIndex >>> 6] |= (1L << (chunkIndex & 63));
        }
    }

    public void freeGrid(int grid_x, int grid_y, Occupant occupant) {
        int index = grid_y * gridSize + grid_x;
        assert occupants[index] == occupant : occupant + " trying to free " + grid_x + " " + grid_y + " where "
                + occupants[index] + " is.";
        occupants[index] = null;
        totalOccupants--;
        int chunkIndex = (grid_y >>> CHUNK_SHIFT) * chunksAcross + (grid_x >>> CHUNK_SHIFT);
        if (--chunkOccupantCounts[chunkIndex] == 0) {
            chunkBitmask[chunkIndex >>> 6] &= ~(1L << (chunkIndex & 63));
        }
    }

    public HeightMap getHeightMap() {
        return heightmap;
    }

    public Optional<Region> findPathRegion(int srcX, int srcY, int dstX, int dstY) {
        Region srcRegion = getRegion(srcX, srcY);
        Region dstRegion = getRegion(dstX, dstY);
        if (srcRegion == null || dstRegion == null) {
            return Optional.empty();
        }
        return pathFinder.findPathRegion(srcRegion, dstRegion);
    }

    Optional<Region> findPathRegion(PathFinderAlgorithm finder, int srcX, int srcY) {
        Region srcRegion = getRegion(srcX, srcY);
        if (srcRegion == null) {
            return Optional.empty();
        }
        return pathFinder.findPathRegion(finder, srcRegion);
    }

    Optional<GridPathNode> findPathGrid(PathFinderAlgorithm finder, int srcX, int srcY) {
        return pathFinder.findPathGrid(finder, srcX, srcY);
    }

    public Optional<GridPathNode> findPathGrid(Region dstRegion, @Nullable Region dstRegion2,
            int srcX, int srcY, int dstX, int dstY,
            @Nullable Target target, float maxDist, boolean allowSecondBest) {
        return pathFinder.findPathGrid(dstRegion, dstRegion2, srcX, srcY, dstX, dstY, target, maxDist, allowSecondBest);
    }

    public int getAndResetPathfindCount() {
        int count = pathFinder.getStatPathfinderPerFrame();
        pathFinder.resetStatPathfinderPerFrame();
        return count;
    }

    PathFinder pathFinder() {
        return pathFinder;
    }
}
