package com.oddlabs.tt.simulation.pathfinder;

import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.SupplyType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Represents a logical sub-region of the unit grid used to optimize pathfinding and object lookups.
 */
public final class Region extends Node {
    private final Set<Building> buildings = new CopyOnWriteArraySet<>();
    private final Map<SupplyType, Set<Supply>> supplies = new EnumMap<>(SupplyType.class);
    private final List<Region> neighbours = new ArrayList<>();

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
    public String toString() {
        return "Region: " + center_x + " " + center_y;
    }

    @Override
    public RegionNode newPath() {
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

    public Set<Building> getBuildings() {
        return buildings;
    }

    public void registerBuilding(Building building) {
        buildings.add(building);
    }

    public boolean unregisterBuilding(Building building) {
        return buildings.remove(building);
    }

    public Set<Supply> getSupplies(SupplyType type) {
        Set<Supply> set = supplies.get(type);
        return set != null ? set : Set.of();
    }

    public void registerSupply(Supply supply) {
        supplies.computeIfAbsent(supply.getSupplyType(), k -> new CopyOnWriteArraySet<>()).add(supply);
    }

    public boolean unregisterSupply(Supply supply) {
        Set<Supply> set = supplies.get(supply.getSupplyType());
        return set != null && set.remove(supply);
    }

    private void addNeighbour(Region n) {
        neighbours.add(n);
    }

    @Override
    public boolean addNeighbours(PathFinderAlgorithm finder, UnitGrid unit_grid) {
        for (Region neighbour : neighbours) {
            if (!neighbour.isVisited())
                unit_grid.pathFinder().addToOpenList(finder, neighbour, this, estimateCost(neighbour.getGridX(),
                        neighbour
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

    public List<Region> getNeighbours() {
        return neighbours;
    }
}
