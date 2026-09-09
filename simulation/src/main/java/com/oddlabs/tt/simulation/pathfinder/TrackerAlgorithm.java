package com.oddlabs.tt.simulation.pathfinder;

import org.jspecify.annotations.Nullable;

/**
 * Defines path tracking strategy interface across high-level regions and grid nodes.
 */
public interface TrackerAlgorithm {
    boolean isDone(int x, int y);

    boolean acceptRegion(Region region);

    @Nullable
    Region findPathRegion(int src_x, int src_y);

    @Nullable
    GridPathNode findPathGrid(Region target_region, Region next_region, int src_x, int src_y,
            boolean allow_secondary_targets);
}
