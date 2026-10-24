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
    private final HeightMap heightmap;
    private final @Nullable Region[][] regions;
    private final @Nullable Occupant[][] occupants;
    private final PathFinder pathFinder = new PathFinder(this);

    public UnitGrid(HeightMap heightmap) {
        this.heightmap = heightmap;
        int unit_grid_size = heightmap.getAccessGrid().length;
        occupants = new Occupant[unit_grid_size][unit_grid_size];
        regions = new Region[unit_grid_size][unit_grid_size];
    }

    private boolean filter(ScanFilter filter, int x, int y) {
        return x >= 0 && y >= 0 && x < occupants.length && y < occupants.length && filter.filter(x, y, occupants[y][x]);
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
        int max_radius = occupants.length;
        while (radius <= max_radius) {
            int x = center_grid_x - radius;
            int x2 = center_grid_x + radius;
            for (int i = 0; i < 2 * radius - 1; i++) {
                int y_i = center_grid_y - radius + 1 + i;
                if (isValidTarget(x, y_i, grid_targets_only)) {
                    return new LandscapeTarget(x, y_i);
                }
                if (isValidTarget(x2, y_i, grid_targets_only)) {
                    return new LandscapeTarget(x2, y_i);
                }
            }
            int y = center_grid_y - radius;
            int y2 = center_grid_y + radius;
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
        return x >= 0 && y >= 0 && x < occupants.length && y < occupants.length
                && (!grid_targets_only || ((x + y) & 1) == 0)
                && occupants[y][x] == null;
    }

    public @Nullable Target[] findGridTargets(int center_grid_x, int center_grid_y, int num_targets,
            boolean grid_targets_only) {
        if (num_targets == 1) {
            Target target = findGridTarget(center_grid_x, center_grid_y, grid_targets_only);
            return new Target[]{target};
        }
        FindTargetsFilter filter = new FindTargetsFilter(num_targets, occupants.length, grid_targets_only);
        scan(filter, center_grid_x, center_grid_y);
        return filter.getTargets();
    }

    public void scan(ScanFilter filter, int center_grid_x, int center_grid_y) {
        int radius = filter.getMinRadius();
        if (radius == 0) {
            if (filter(filter, center_grid_x, center_grid_y))
                return;
            radius++;
        }
        while (radius <= filter.getMaxRadius()) {
            int x = center_grid_x - radius;
            int x2 = center_grid_x + radius;
            for (int i = 0; i < 2 * radius - 1; i++) {
                int y_i = center_grid_y - radius + 1 + i;
                if (filter(filter, x, y_i) || filter(filter, x2, y_i))
                    return;
            }
            int y = center_grid_y - radius;
            int y2 = center_grid_y + radius;
            for (int i = 0; i < 2 * radius + 1; i++) {
                int x_i = center_grid_x - radius + i;
                if (filter(filter, x_i, y) || filter(filter, x_i, y2))
                    return;
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
        return occupants.length;
    }

    public @Nullable Region getRegion(int grid_x, int grid_y) {
        Region region = regions[grid_y][grid_x];
        return region;
    }

    public void setRegion(int grid_x, int grid_y, Region r) {
        assert regions[grid_y][grid_x] == null && !isGridOccupied(grid_x, grid_y);
        regions[grid_y][grid_x] = r;
    }

    public boolean isGridOccupied(int grid_x, int grid_y) {
        return occupants[grid_y][grid_x] != null;
    }

    public @Nullable Occupant getOccupant(int grid_x, int grid_y) {
        return occupants[grid_y][grid_x];
    }

    public void occupyGrid(int grid_x, int grid_y, Occupant occupant) {
        assert !isGridOccupied(grid_x, grid_y);
        occupants[grid_y][grid_x] = occupant;
    }

    public void freeGrid(int grid_x, int grid_y, Occupant occupant) {
        assert occupants[grid_y][grid_x] == occupant : occupant + " trying to free " + grid_x + " " + grid_y + " where "
                + occupants[grid_y][grid_x] + " is.";
        occupants[grid_y][grid_x] = null;
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
