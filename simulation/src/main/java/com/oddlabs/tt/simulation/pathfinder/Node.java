package com.oddlabs.tt.simulation.pathfinder;

import org.jspecify.annotations.Nullable;

/**
 * Base abstract node for graph search and A* pathfinding algorithms.
 */
abstract class Node {
    /** terminal node */
    static final Node TOMBSTONE = new Node() {
        @Override
        public boolean addNeighbours(PathFinderAlgorithm finder, UnitGrid unit_grid) {
            return false;
        }

        @Override
        public @Nullable PathNode<?> newPath() {
            return null;
        }

        @Override
        public int getGridX() {
            return -1;
        }

        @Override
        public int getGridY() {
            return -1;
        }
    };

    /*	private int x;
        private int y;*/
    private int cost;
    private int total_cost;
    private @Nullable Node parent;
    private int search_id;

    protected Node() {
    }

    public final @Nullable Node getParent() {
        return parent;
    }

    public final void resetSearchId() {
        search_id = 0;
    }

    public final void setPathInitial(int search_id, int initial_cost) {
        this.search_id = search_id;
        cost = 0;
        parent = null;
        total_cost = initial_cost;
    }

    public final void setPath(int search_id, Node parent, int cost, int estimated_cost) {
        assert this.search_id != search_id;
        this.search_id = search_id;
        this.cost = parent.cost + cost;
        this.parent = parent;
        total_cost = this.cost + estimated_cost;
    }

    public final int getTotalCost() {
        return total_cost;
    }

    public final boolean isVisited(int search_id) {
        return this.search_id == search_id;
    }

    public final int estimateCost(int dest_x, int dest_y) {
        int vec_x = dest_x - getGridX();
        int vec_y = dest_y - getGridY();
        int abs_dx = Math.abs(vec_x);
        int abs_dy = Math.abs(vec_y);
        int max;
        int min;
        if (abs_dx > abs_dy) {
            max = abs_dx;
            min = abs_dy;
        } else {
            max = abs_dy;
            min = abs_dx;
        }
        return RegionBuilder.STRAIGHT * (max - min) + RegionBuilder.DIAGONAL * min;
//		return Math.max(Math.abs(vec_x), Math.abs(vec_y));
//		return Math.abs(vec_x) + Math.abs(vec_y);
    }

    public abstract boolean addNeighbours(PathFinderAlgorithm finder, UnitGrid unit_grid);

    public abstract @Nullable PathNode<?> newPath();

    public abstract int getGridX();

    public abstract int getGridY();
}
