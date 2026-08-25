package com.oddlabs.tt.simulation.landscape;


import com.oddlabs.tt.base.geom.BoundingBox;
import org.jspecify.annotations.Nullable;

public abstract sealed class AbstractPatchGroup extends BoundingBox permits PatchGroup, LandscapeLeaf {
    private final @Nullable AbstractPatchGroup parent;

    protected AbstractPatchGroup(HeightMap heightmap, float patch_size, int x, int y,
            @Nullable AbstractPatchGroup parent) {
        this.parent = parent;
    }

    final void editHeight(float height) {
        checkBoundsZ(height);
        if (parent != null)
            parent.editHeight(height);
    }

    final void setBoundsFromLandscape(HeightMap heightmap, int start_x, int start_y, int size_x, int size_y) {
        float corner1 = heightmap.getWrappedHeight(start_x, start_y);
        setBounds(start_x * HeightMap.METERS_PER_UNIT_GRID, (start_x + size_x) * HeightMap.METERS_PER_UNIT_GRID, start_y
                * HeightMap.METERS_PER_UNIT_GRID, (start_y + size_y) * HeightMap.METERS_PER_UNIT_GRID, corner1,
                corner1);
        for (int grid_y = 0; grid_y <= size_x; grid_y++) {
            for (int grid_x = 0; grid_x <= size_y; grid_x++) {
                float height = heightmap.getWrappedHeight(start_x + grid_x, start_y + grid_y);
                checkBoundsZ(height);
            }
        }
    }
}
