package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.model.Selectable;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Pathfinding tracker algorithm that searches for dynamic occupants matching a filter.
 */
/**
 * Pathfinding tracker algorithm that searches for dynamic occupants matching a filter.
 */
public final class FinderTrackerAlgorithm<O extends Occupant> implements TrackerAlgorithm {
    private final FinderFilter<O> filter;
    private final UnitGrid unit_grid;
    private @Nullable O target;

    public FinderTrackerAlgorithm(UnitGrid unit_grid, FinderFilter<O> filter) {
        this.unit_grid = unit_grid;
        this.filter = filter;
    }

    @Override
    public boolean isDone(int x, int y) {
        return target != null && !target.isDead() && Selectable.isCloseEnough(unit_grid, 0f, x, y, target);
    }

    @Override
    public boolean acceptRegion(Region region) {
        return filter.getOccupantFromRegion(region, true).isPresent();
    }

    @Override
    public Optional<Region> findPathRegion(int src_x, int src_y) {
        TargetRegionFinder region_finder = new TargetRegionFinder(unit_grid, filter);
        return PathFinder.findPathRegion(unit_grid, region_finder, unit_grid.getRegion(src_x, src_y));
    }

    public Optional<O> getOccupant() {
        return target == null || target.isDead() ? Optional.empty() : Optional.of(target);
    }

    @Override
    public Optional<GridPathNode> findPathGrid(Region target_region, Region next_region, int src_x,
            int src_y, boolean allow_secondary_targets) {
        var hint_occupant = filter.getOccupantFromRegion(target_region, true);
        return hint_occupant.map(hint -> {
            TargetFinderAlgorithm<O> grid_finder = new TargetFinderAlgorithm<>(
                    unit_grid, filter, next_region, hint.getGridX(), hint.getGridY(), allow_secondary_targets
            );
            var path = PathFinder.findPathGrid(unit_grid, grid_finder, src_x, src_y);
            target = grid_finder.getOccupant();
            return path.orElse(null);
        });
    }
}
