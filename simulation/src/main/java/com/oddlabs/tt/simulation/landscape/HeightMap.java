package com.oddlabs.tt.simulation.landscape;

import java.util.List;

/**
 * Manages spatial terrain elevation, surface interpolation, accessibility, and building grid queries.
 */
public final class HeightMap implements LandscapeEnvironment {
    public static final int METERS_PER_UNIT_GRID = 2;
    public static final int GRID_UNITS_PER_PATCH_EXP = 4;
    public static final int GRID_UNITS_PER_PATCH = 1 << GRID_UNITS_PER_PATCH_EXP;

    private final World world_instance;
    private final LandscapeData landscapeData;
    private final int patches_per_world;
    private final int meters_per_patch;
    private final int grid_units_per_world;
    private final float inv_meters_per_patch;
    private final float inv_meters_per_grid_unit;

    private final LandscapeLeaf[][] landscape_leaves;

    public HeightMap(World world_instance, LandscapeData landscapeData) {
        this.world_instance = world_instance;
        this.landscapeData = landscapeData;
        this.grid_units_per_world = (int) Math.sqrt(landscapeData.heightmap().length);
        patches_per_world = grid_units_per_world / GRID_UNITS_PER_PATCH;
        meters_per_patch = GRID_UNITS_PER_PATCH * METERS_PER_UNIT_GRID;
        inv_meters_per_patch = 1f / meters_per_patch;
        inv_meters_per_grid_unit = 1f / METERS_PER_UNIT_GRID;

        landscape_leaves = new LandscapeLeaf[patches_per_world][patches_per_world];
    }


    public float[] getHeightData() {
        return landscapeData.heightmap();
    }

    public LandscapeData getLandscapeData() {
        return landscapeData;
    }

    public World getWorld() {
        return world_instance;
    }

    public boolean isGridInside(int x, int y) {
        boolean inside_world = x >= 0 && y >= 0 && x < getGridUnitsPerWorld() && y < getGridUnitsPerWorld();
        return inside_world;
    }

    public boolean isInside(float x, float y) {
        boolean inside_world = x >= 0 && y >= 0 && x <= getMetersPerWorld() && y <= getMetersPerWorld();
        return inside_world;
    }

    @Override
    public int getMetersPerWorld() {
        return landscapeData.metersPerWorld();
    }

    public int getGridUnitsPerPatch() {
        return GRID_UNITS_PER_PATCH;
    }

    public float getInvMetersPerGridUnit() {
        return inv_meters_per_grid_unit;
    }

    @Override
    public int getPatchesPerWorld() {
        return patches_per_world;
    }

    @Override
    public int getMetersPerPatch() {
        return meters_per_patch;
    }

    @Override
    public int getGridUnitsPerWorld() {
        return grid_units_per_world;
    }

    @Override
    public float getSeaLevelMeters() {
        return landscapeData.seaLevelMeters();
    }

    void registerLeaf(int x, int y, LandscapeLeaf leaf) {
        landscape_leaves[y][x] = leaf;
    }

    public List<int[]> getTrees() {
        return landscapeData.trees();
    }

    public boolean[][] getAccessGrid() {
        return landscapeData.accessGrid();
    }


    @Override
    public boolean isBelowSeaLevel(int patch_x, int patch_y) {
        int offset_x = patch_x * getGridUnitsPerPatch();
        int offset_y = patch_y * getGridUnitsPerPatch();
        for (int y = 0; y <= getGridUnitsPerPatch(); y++) {
            for (int x = 0; x <= getGridUnitsPerPatch(); x++) {
                float height = getWrappedHeight(offset_x + x, offset_y + y);
                if (height < getSeaLevelMeters())
                    return true;
            }
        }
        return false;
    }

    public LandscapeLeaf getLeafFromCoordinates(float x_f, float y_f) {
        int patch_x = coordinateToPatch(x_f);
        int patch_y = coordinateToPatch(y_f);
        return landscape_leaves[patch_y][patch_x];
    }

    public int coordinateToPatch(float f) {
        return (int) (f * inv_meters_per_patch);
    }

    public float computeInterpolatedHeight(int lod, float x_f, float y_f) {
        x_f *= inv_meters_per_grid_unit;
        y_f *= inv_meters_per_grid_unit;

        int size = getGridUnitsPerWorld();
        x_f = (x_f % size + size) % size;
        y_f = (y_f % size + size) % size;

        int x0 = (int) x_f;
        int y0 = (int) y_f;
        int x1 = (x0 + 1) % size;
        int y1 = (y0 + 1) % size;

        float dx = x_f - x0;
        float dy = y_f - y0;

        float[] heightmap = landscapeData.heightmap();
        float h00 = heightmap[y0 * size + x0];
        float h10 = heightmap[y0 * size + x1];
        float h01 = heightmap[y1 * size + x0];
        float h11 = heightmap[y1 * size + x1];

        return dx + dy < 1.0f
               ? h00 + dx * (h10 - h00) + dy * (h01 - h00)
               : h11 + (1.0f - dx) * (h01 - h11) + (1.0f - dy) * (h10 - h11);
    }

    @Override
    public float getHeight(float x, float y) {
        return computeInterpolatedHeight(0, x, y);
    }

    public float getNearestHeight(float x_f, float y_f) {
        return computeInterpolatedHeight(0, x_f, y_f);
    }

    public float getClampedHeight(int grid_x, int grid_y) {
        if (grid_x < 0 || grid_x >= grid_units_per_world)
            grid_x = 0;
        if (grid_y < 0 || grid_y >= grid_units_per_world)
            grid_y = 0;

        return getHeight(grid_x, grid_y);
    }

    public byte getBuildValue(int grid_x, int grid_y) {
        return landscapeData.buildGrid()[grid_y][grid_x];
    }

    public boolean canBuild(int grid_x, int grid_y, int val) {
        grid_x = wrapGridCoord(grid_x);
        grid_y = wrapGridCoord(grid_y);
        return landscapeData.buildGrid()[grid_y][grid_x] >= val;
    }

    public float getWrappedHeight(int grid_x, int grid_y) {
        grid_x = wrapGridCoord(grid_x);
        grid_y = wrapGridCoord(grid_y);
        return getHeight(grid_x, grid_y);
    }

    private int wrapGridCoord(int coord) {
        int size = getGridUnitsPerWorld();
        return (coord % size + size) % size;
    }

    public float getHeight(int grid_x, int grid_y) {
        return landscapeData.heightmap()[grid_y * grid_units_per_world + grid_x];
    }

    public void editHeight(int grid_x, int grid_y, float height) {
        final int wrappedX = wrapGridCoord(grid_x);
        final int wrappedY = wrapGridCoord(grid_y);
        landscapeData.heightmap()[wrappedY * grid_units_per_world + wrappedX] = height;

        int patch_x1 = wrappedX / GRID_UNITS_PER_PATCH;

        int patch_y1 = wrappedY / GRID_UNITS_PER_PATCH;
        boolean x_border = patch_x1 * GRID_UNITS_PER_PATCH == wrappedX;
        boolean y_border = patch_y1 * GRID_UNITS_PER_PATCH == wrappedY;
        int patch_x0 = (patch_x1 - (x_border ? 1 : 0) + patches_per_world) % patches_per_world;
        int patch_y0 = (patch_y1 - (y_border ? 1 : 0) + patches_per_world) % patches_per_world;

        for (int y = patch_y0;; y = (y + 1) % patches_per_world) {
            for (int x = patch_x0;; x = (x + 1) % patches_per_world) {
                landscape_leaves[y][x].editHeight(height);
                if (x == patch_x1) break;
            }
            if (y == patch_y1) break;
        }
        world_instance.getNotificationListener().patchesEdited(patch_x0, patch_y0, patch_x1, patch_y1);
    }
}
