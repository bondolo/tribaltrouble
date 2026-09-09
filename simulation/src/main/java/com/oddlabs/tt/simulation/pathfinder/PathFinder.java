package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.base.util.PocketList;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core pathfinding routines computing optimal region and grid navigation paths.
 */
public final class PathFinder {
    private static final PocketList<Node> open_list = new PocketList<>(RegionBuilder.MAX_PATH_COST);
    private static final List<Node> visited_list = new ArrayList<>();
    public static int stat_pathfinder_per_frame = 0;

    public static Optional<Region> findPathRegion(UnitGrid unit_grid, Region src_region, Region dst_region) {
        /*		Node src_region = unit_grid.getRegion(src_grid_x, src_grid_y);
        		Node dst_region = unit_grid.getRegion(dst_grid_x, dst_grid_y);*/
        assert src_region != null;// : "src_grid_x = " + src_grid_x + " | src_grid_y = " + src_grid_y;
        assert dst_region != null;// : "dst_grid_x = " + dst_grid_x + " | dst_grid_y = " + dst_grid_y;
        PathFinderAlgorithm finder = new RegionPathFinder(unit_grid, dst_region);
        return doFindPath(finder, src_region, unit_grid).map(Region.class::cast);
    }

    public static Optional<Region> findPathRegion(UnitGrid unit_grid, PathFinderAlgorithm finder,
            Region current_region) {
//		Node current_region = UnitGrid.getGrid().getRegion(src_grid_x, src_grid_y);
        assert current_region != null;// : "src_grid_x = " + src_grid_x + " | src_grid_y = " + src_grid_y + " | occupant " + UnitGrid.getGrid().getOccupant(src_grid_x, src_grid_y);
        return doFindPath(finder, current_region, unit_grid).map(Region.class::cast);
    }

    public static Optional<GridPathNode> findPathGrid(UnitGrid unit_grid, PathFinderAlgorithm finder,
            int src_grid_x, int src_grid_y) {
        GridNode.Offset offset = GridNode.setupPathFinding(src_grid_x, src_grid_y, src_grid_x, src_grid_y);
        if (offset == null)
            return Optional.empty();
        Node current_node = GridNode.getPathfinderNode(offset, src_grid_x, src_grid_y);
        var grid_node = doFindPath(finder, current_node, unit_grid);
        return grid_node.map(gn -> (GridPathNode) gn.newPath());
    }

    public static Optional<GridPathNode> findPathGrid(UnitGrid unit_grid, Region dst_region,
            @Nullable Region dst_region2, int src_grid_x, int src_grid_y, int dst_grid_x, int dst_grid_y, Target target,
            float max_dist, boolean allow_second_best) {
        GridNode.Offset offset = GridNode.setupPathFinding(src_grid_x, src_grid_y, src_grid_x, src_grid_y);
        if (offset == null)
            return Optional.empty();
        Node current_node = GridNode.getPathfinderNode(offset, src_grid_x, src_grid_y);
        PathFinderAlgorithm finder = new TargetGridPathFinder(unit_grid, max_dist, dst_region, dst_region2, dst_grid_x,
                dst_grid_y, target, allow_second_best);
        var grid_node = doFindPath(finder, current_node, unit_grid);
        return grid_node.map(gn -> (GridPathNode) gn.newPath());
    }

    private static Optional<Node> doFindPath(PathFinderAlgorithm finder, @Nullable Node start_node,
            UnitGrid unit_grid) {
        if (start_node == null)
            return Optional.empty();
        Node current_node = start_node;
        stat_pathfinder_per_frame++;
        initSearch();
        current_node.setPathInitial(finder.computeEstimatedCost(current_node));
        addToLists(current_node);
        while (!open_list.isEmpty()) {
            current_node = open_list.removeBest();
            var result = finder.touchNode(current_node);
            if (result.isPresent())
                return result.filter(node -> node != Node.TOMBSTONE);
            boolean neighbour_result = current_node.addNeighbours(finder, unit_grid);
            if (neighbour_result)
                return Optional.of(current_node);
        }
        return finder.getBestNode().filter(node -> node != Node.TOMBSTONE);
    }

    public static void addToOpenList(PathFinderAlgorithm finder, Node current_node,
            Node parent, int cost) {
        current_node.setPath(parent, cost, finder.computeEstimatedCost(current_node));
        addToLists(current_node);
    }

    private static void addToLists(Node current_node) {
        open_list.add(current_node.getTotalCost(), current_node);
        visited_list.add(current_node);
    }

    private static void initSearch() {
        open_list.clear();
        visited_list.forEach(Node::reset);
        visited_list.clear();
    }

    private PathFinder() {
        // no instances
    }
}
