package com.oddlabs.tt.simulation.pathfinder;


import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Represents a logical sub-region of the unit grid used to optimize pathfinding and object lookups.
 */
public final class Region extends Node {
    private final Map<@NonNull Class<?>, @NonNull Set<?>> objectSets = new HashMap<>();
    private final List<@NonNull Region> neighbours = new ArrayList<>();

    private int center_x;
    private int center_y;

    @Override
    public int getGridX() {
        return center_x;
    }

    @Override
    public int getGridY() {
        return center_y;
    }

    public void setPosition(int center_x, int center_y) {
        this.center_x = center_x;
        this.center_y = center_y;
    }

    @Override
    public @NonNull String toString() {
        return "Region: " + center_x + " " + center_y;
    }

    @Override
    public @NonNull PathNode newPath() {
        Node graph_node = this;
        assert graph_node != null;
        RegionNode current_node = null;
        while (graph_node != null) {
            current_node = new RegionNode(current_node, (Region) graph_node);
            graph_node = graph_node.getParent();
        }
        return current_node;
    }

    public static void link(@Nullable Region r1, @Nullable Region r2) {
        if (r1 == null || r2 == null || r1 == r2 || r1.neighbours.contains(r2))
            return;
        r1.addNeighbour(r2);
        r2.addNeighbour(r1);
    }

    public <K> @NonNull Set<K> getObjects(@NonNull Class<? super K> key) {
        //noinspection unchecked
        return (Set<K>) objectSets.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>());
    }

    public <K> void registerObject(@NonNull Class<? super K> key, K object) {
        getObjects(key).add(object);
    }

    public <K> boolean unregisterObject(@NonNull Class<? super K> key, K object) {
        @SuppressWarnings("unchecked") Set<K> list = (Set<K>) objectSets.get(key);
        assert list != null : "Unknown key";
        return null != list && list.remove(object);
    }

    private void addNeighbour(@NonNull Region n) {
        neighbours.add(n);
    }

    @Override
    public boolean addNeighbours(@NonNull PathFinderAlgorithm finder, UnitGrid unit_grid) {
        for (Region neighbour : neighbours) {
            if (!neighbour.isVisited())
                PathFinder.addToOpenList(finder, neighbour, this, estimateCost(neighbour.getGridX(), neighbour
                        .getGridY()));
        }
        return false;
    }

    public void markVisited() {
        setVisited(true);
    }

    public void clearVisited() {
        setVisited(false);
    }

    public @NonNull List<@NonNull Region> getNeighbours() {
        return neighbours;
    }
}
