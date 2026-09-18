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
final class PathFinder {
    private final UnitGrid unit_grid;
    private final PocketList<Node> open_list = new PocketList<>(RegionBuilder.MAX_PATH_COST);
    private final List<Node> visited_list = new ArrayList<>();
    private final GridNode[][] pathfinder_grid = new GridNode[RegionBuilder.GRID_SIZE][RegionBuilder.GRID_SIZE];
    private int stat_pathfinder_per_frame = 0;

    PathFinder(UnitGrid unit_grid) {
        this.unit_grid = unit_grid;
        for (int y = 0; y < pathfinder_grid.length; y++) {
            for (int x = 0; x < pathfinder_grid.length; x++) {
                pathfinder_grid[y][x] = new GridNode(x, y);
            }
        }
    }

    @Nullable
    GridNode getPathfinderNode(GridNode.Offset offset, int x, int y) {
        int local_x = x - offset.offset_x();
        int local_y = y - offset.offset_y();
        if (local_x < 0 || local_x >= pathfinder_grid.length ||
                local_y < 0 || local_y >= pathfinder_grid.length) {
            return null;
        }
        GridNode node = pathfinder_grid[local_y][local_x];
        node.setOffset(offset);
        return node;
    }

    Optional<Region> findPathRegion(Region src_region, Region dst_region) {
        assert src_region != null;
        assert dst_region != null;
        PathFinderAlgorithm finder = new RegionPathFinder(unit_grid, dst_region);
        return doFindPath(finder, src_region).map(Region.class::cast);
    }

    Optional<Region> findPathRegion(PathFinderAlgorithm finder, Region current_region) {
        assert current_region != null;
        return doFindPath(finder, current_region).map(Region.class::cast);
    }

    Optional<GridPathNode> findPathGrid(PathFinderAlgorithm finder, int src_grid_x, int src_grid_y) {
        GridNode.Offset offset = GridNode.setupPathFinding(src_grid_x, src_grid_y, src_grid_x, src_grid_y);
        if (offset == null) {
            return Optional.empty();
        }
        Node current_node = getPathfinderNode(offset, src_grid_x, src_grid_y);
        var grid_node = doFindPath(finder, current_node);
        return grid_node.map(GridNode.class::cast).map(GridNode::newPath);
    }

    Optional<GridPathNode> findPathGrid(Region dst_region, @Nullable Region dst_region2,
            int src_grid_x, int src_grid_y, int dst_grid_x, int dst_grid_y,
            @Nullable Target target, float max_dist, boolean allow_second_best) {
        GridNode.Offset offset = GridNode.setupPathFinding(src_grid_x, src_grid_y, src_grid_x, src_grid_y);
        if (offset == null) {
            return Optional.empty();
        }
        Node current_node = getPathfinderNode(offset, src_grid_x, src_grid_y);
        PathFinderAlgorithm finder = new TargetGridPathFinder(unit_grid, max_dist, dst_region, dst_region2, dst_grid_x,
                dst_grid_y, target, allow_second_best);
        var grid_node = doFindPath(finder, current_node);
        return grid_node.map(GridNode.class::cast).map(GridNode::newPath);
    }

    private Optional<Node> doFindPath(PathFinderAlgorithm finder, @Nullable Node start_node) {
        if (start_node == null) {
            return Optional.empty();
        }
        Node current_node = start_node;
        stat_pathfinder_per_frame++;
        initSearch();
        current_node.setPathInitial(finder.computeEstimatedCost(current_node));
        addToLists(current_node);
        while (!open_list.isEmpty()) {
            current_node = open_list.removeBest();
            var result = finder.touchNode(current_node);
            if (result.isPresent()) {
                return result.filter(node -> node != Node.TOMBSTONE);
            }
            boolean neighbour_result = current_node.addNeighbours(finder, unit_grid);
            if (neighbour_result) {
                return Optional.of(current_node);
            }
        }
        return finder.getBestNode().filter(node -> node != Node.TOMBSTONE);
    }

    void addToOpenList(PathFinderAlgorithm finder, Node current_node, Node parent, int cost) {
        current_node.setPath(parent, cost, finder.computeEstimatedCost(current_node));
        addToLists(current_node);
    }

    int getStatPathfinderPerFrame() {
        return stat_pathfinder_per_frame;
    }

    void resetStatPathfinderPerFrame() {
        stat_pathfinder_per_frame = 0;
    }

    private void addToLists(Node current_node) {
        open_list.add(current_node.getTotalCost(), current_node);
        visited_list.add(current_node);
    }

    private void initSearch() {
        open_list.clear();
        visited_list.forEach(Node::reset);
        visited_list.clear();
    }
}
