package com.oddlabs.tt.simulation.pathfinder;

import org.jspecify.annotations.Nullable;

/**
 * Grid pathfinder searching for occupants matching a specific filter.
 */
final class TargetFinderAlgorithm<O extends Occupant> extends GridPathFinder {
    private final FinderFilter<O> filter;

    private @Nullable O target;

    TargetFinderAlgorithm(UnitGrid unit_grid, FinderFilter<O> filter, Node dst_region, int dst_x,
            int dst_y, boolean allow_second_best) {
        super(unit_grid, dst_region, null, dst_x, dst_y, allow_second_best);
        this.filter = filter;
    }

    @Nullable
    O getOccupant() {
        return target;
    }

    @Override
    public boolean touchNeighbour(Occupant occ) {
        if (filter.acceptOccupant(occ)) {
            //noinspection unchecked
            target = (O) occ;
            return true;
        } else
            return false;
    }
}
