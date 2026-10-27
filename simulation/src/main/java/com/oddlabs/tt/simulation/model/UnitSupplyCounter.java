package com.oddlabs.tt.simulation.model;

/**
 * Tracks live and pending quantities of living units residing inside a building.
 */
final class UnitSupplyCounter extends SupplyCounter {
    UnitSupplyCounter(Building building) {
        super(building);
    }

    @Override
    public int getNumSupplies() {
        Building b = getBuilding();
        return !b.isDead() ? b.getUnitContainer().map(SupplyContainer::getNumSupplies).orElse(0) + getDelta() : 0;
    }

    @Override
    public int getMaxSupplies() {
        Building b = getBuilding();
        return !b.isDead() ? b.getUnitContainer().map(SupplyContainer::getMaxSupplyCount).orElse(0) : 0;
    }
}
