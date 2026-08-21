package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

public final class UnitGrid {
    private final Region[][] regions;
    private final @Nullable Occupant[][] occupants;
    private final HeightMap heightmap;

    public UnitGrid(HeightMap heightmap) {
        this.heightmap = heightmap;
        int unit_grid_size = heightmap.getAccessGrid().length;
        occupants = new Occupant[unit_grid_size][unit_grid_size];
        regions = new Region[unit_grid_size][unit_grid_size];
    }

    private boolean filter(ScanFilter filter, int x, int y) {
        return x >= 0 && y >= 0 && x < occupants.length && y < occupants.length && filter.filter(x, y, occupants[y][x]);
    }

    public @Nullable Target[] findGridTargets(int center_grid_x, int center_grid_y, int num_targets,
            boolean grid_targets_only) {
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

    public Region getRegion(int grid_x, int grid_y) {
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
}
