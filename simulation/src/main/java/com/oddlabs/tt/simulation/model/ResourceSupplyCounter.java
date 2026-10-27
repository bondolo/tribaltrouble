package com.oddlabs.tt.simulation.model;

/**
 * Tracks live and pending quantities of raw harvestable resources in a building.
 */
final class ResourceSupplyCounter extends SupplyCounter {
    private final SupplyType supplyType;

    ResourceSupplyCounter(Building building, SupplyType supplyType) {
        super(building);
        this.supplyType = supplyType;
    }

    SupplyType getSupplyType() {
        return supplyType;
    }

    @Override
    public int getNumSupplies() {
        Building b = getBuilding();
        return !b.isDead() ? b.getSupplyContainer(supplyType).map(SupplyContainer::getNumSupplies).orElse(0)
                + getDelta()
                : 0;
    }

    @Override
    public int getMaxSupplies() {
        Building b = getBuilding();
        return !b.isDead() ? b.getSupplyContainer(supplyType).map(SupplyContainer::getMaxSupplyCount).orElse(0) : 0;
    }
}
