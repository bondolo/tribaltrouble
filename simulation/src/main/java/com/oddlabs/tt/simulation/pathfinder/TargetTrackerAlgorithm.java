package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Pathfinding tracker algorithm that tracks paths towards a specific target.
 */
public final class TargetTrackerAlgorithm implements TrackerAlgorithm {
    private final UnitGrid unit_grid;
    private final Target target;
    private final float max_dist;

    public TargetTrackerAlgorithm(UnitGrid unit_grid, float max_dist, Target target) {
        this.unit_grid = unit_grid;
        this.max_dist = max_dist;
        this.target = target;
    }

    @Override
    public boolean isDone(int x, int y) {
        return target.isDead() || Selectable.isCloseEnough(unit_grid, max_dist, x, y, target);
    }

    @Override
    public boolean acceptRegion(Region region) {
        return !target.isDead() && unit_grid.getRegion(target.getGridX(), target.getGridY()) == region;
    }

    @Override
    public Optional<Region> findPathRegion(int src_x, int src_y) {
        return target.isDead()
                ? Optional.empty()
                : PathFinder.findPathRegion(unit_grid,
                        unit_grid.getRegion(src_x, src_y),
                        unit_grid.getRegion(target.getGridX(), target.getGridY()));
    }

    @Override
    public Optional<GridPathNode> findPathGrid(Region target_region, Region next_region, int src_x,
            int src_y, boolean allow_secondary_targets) {
        return target.isDead()
                ? Optional.empty()
                : PathFinder.findPathGrid(unit_grid, next_region, null,
                        src_x, src_y,
                        target.getGridX(), target.getGridY(),
                        target, max_dist, allow_secondary_targets);
    }
}
