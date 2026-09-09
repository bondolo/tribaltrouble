package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.FinderFilter;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.Region;
import com.oddlabs.tt.simulation.pathfinder.RegionBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Filter strategy for locating candidate resource supplies of a specific type closest to a unit.
 */
final class SupplyFinder<S extends Supply> implements FinderFilter<S> {
    private final Unit unit;
    private final SupplyType supplyType;
    private final Set<Set<S>> regions = new CopyOnWriteArraySet<>();
    private int max_region_dist_sqr;

    SupplyFinder(Unit unit, SupplyType supplyType) {
        this.unit = unit;
        this.supplyType = supplyType;
    }

    @Override
    public Optional<S> getOccupantFromRegion(Region region, boolean one_region) {
        @SuppressWarnings("unchecked")
        Class<S> supplyClass = (Class<S>) supplyType.getSupplyClass();
        Set<S> supplies = region.getObjects(supplyClass);
        if (one_region) {
            return findClosest(supplies);
        } else {
            int dx = region.getGridX() - unit.getGridX();
            int dy = region.getGridY() - unit.getGridY();
            int region_dist_sqr = dx * dx + dy * dy;
            if (!supplies.isEmpty()) {
                if (regions.isEmpty()) {
                    int region_dist = (int) Math.sqrt(region_dist_sqr);
                    int max_region_dist = region_dist + RegionBuilder.REGION_PATH_MAX_COST / 2;
                    max_region_dist_sqr = max_region_dist * max_region_dist;
                }
                regions.add(supplies);
            }
            if (!regions.isEmpty() && region_dist_sqr > max_region_dist_sqr) {
                return findClosest();
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<S> getBest() {
        return findClosest();
    }

    private Optional<S> findClosest(Set<S> supplies) {
        return supplies.stream().min(Comparator.comparingInt(this::distanceSquared));
    }

    private Optional<S> findClosest() {
        Optional<S> closest = regions.stream()
                .flatMap(Set::stream)
                .min(Comparator.comparingInt(this::distanceSquared));
        regions.clear();
        return closest;
    }

    private int distanceSquared(S supply) {
        int dx = supply.getGridX() - unit.getGridX();
        int dy = supply.getGridY() - unit.getGridY();
        return dx * dx + dy * dy;
    }

    @Override
    public boolean acceptOccupant(Occupant occ) {
        if (supplyType.getSupplyClass().isInstance(occ)) {
            Supply supply = (Supply) occ;
            assert !supply.isEmpty();
            return true;
        } else
            return false;
    }
}
