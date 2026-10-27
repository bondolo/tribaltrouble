package com.oddlabs.tt.simulation.model;

/**
 * Tracks and computes live and pending supply counts for building UI indicators.
 */
public abstract sealed class SupplyCounter permits WeaponSupplyCounter, ResourceSupplyCounter, UnitSupplyCounter {
    private final Building building;
    private int delta = 0;

    protected SupplyCounter(Building building) {
        this.building = building;
    }

    public static SupplyCounter of(Building building, WeaponTier tier) {
        return new WeaponSupplyCounter(building, tier);
    }

    public static SupplyCounter of(Building building, SupplyType supplyType) {
        return new ResourceSupplyCounter(building, supplyType);
    }

    public static SupplyCounter ofUnits(Building building) {
        return new UnitSupplyCounter(building);
    }

    public final Building getBuilding() {
        return building;
    }

    public final void increaseSupply(int amount) {
        delta += amount;
    }

    public final int getDelta() {
        return delta;
    }

    public abstract int getNumSupplies();

    public abstract int getMaxSupplies();
}
