package com.oddlabs.tt.simulation.pathfinder;

import java.util.Optional;

/**
 * Search algorithm locating valid target regions containing matching occupants.
 */
final class TargetRegionFinder implements PathFinderAlgorithm {
    private final FinderFilter<?> filter;
    private final UnitGrid unit_grid;

    TargetRegionFinder(UnitGrid unit_grid, FinderFilter<?> filter) {
        this.unit_grid = unit_grid;
        this.filter = filter;
    }

    @Override
    public int computeEstimatedCost(Node node) {
        return 0;
    }

    @Override
    public boolean touchNeighbour(Occupant occ) {
        return false;
    }

    @Override
    public Optional<Node> touchNode(Node node) {
        Region region = (Region) node;
        var occ = filter.getOccupantFromRegion(region, false);
        return occ.map(o -> unit_grid.getRegion(o.getGridX(), o.getGridY()));
    }

    @Override
    public Optional<Node> getBestNode() {
        return filter.getBest().map(o -> unit_grid.getRegion(o.getGridX(), o.getGridY()));
    }
}
