package com.oddlabs.tt.simulation.model;

/**
 * Tracks live and pending quantities of manufactured weapons in a building armory.
 */
final class WeaponSupplyCounter extends SupplyCounter {
    private final WeaponTier tier;

    WeaponSupplyCounter(Building building, WeaponTier tier) {
        super(building);
        this.tier = tier;
    }

    WeaponTier getTier() {
        return tier;
    }

    @Override
    public int getNumSupplies() {
        Building b = getBuilding();
        return !b.isDead() ? b.getWeaponSupplyCount(tier) + getDelta() : 0;
    }

    @Override
    public int getMaxSupplies() {
        Building b = getBuilding();
        return !b.isDead() ? b.getWeaponContainer(tier).map(SupplyContainer::getMaxSupplyCount).orElse(0) : 0;
    }
}
