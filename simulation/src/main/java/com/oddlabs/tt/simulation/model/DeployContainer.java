package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.Nullable;

/**
 * Container managing the deployment timing, order queues, and resource/unit consumption for a deployable unit type.
 */
public final class DeployContainer {
    private final Building building;
    private final DeployType deploy_type;
    private final @Nullable SupplyContainer required_supply;
    private final float seconds_per_deploy;

    private float time = 0;
    private int queued_count = 0;
    private int num_orders = 0;

    DeployContainer(Building building, float seconds_per_deploy, DeployType deploy_type,
            @Nullable SupplyContainer required_supply) {
        this.building = building;
        this.seconds_per_deploy = seconds_per_deploy;
        this.deploy_type = deploy_type;
        this.required_supply = required_supply;
    }

    /**
     * {@return the required supply container (e.g. weapons for warriors or raw materials for transporters), or null if
     * none}
     */
    public @Nullable SupplyContainer getRequiredSupply() {
        return required_supply;
    }

    private int capAmount(int amount) {
        return Math.max(amount, -queued_count);
    }

    public int getNumSupplies() {
        return queued_count;
    }

    public int increaseSupply(int amount) {
        int result = building.getOwner().getUnitCountContainer().increaseSupply(amount);
        assert result == amount;
        queued_count += amount;
        return amount;
    }

    public void orderSupply(int orders) {
        int capped_amount = capAmount(orders);
        UnitContainer unitContainer = building.getUnitContainer().orElseThrow();
        int result = -unitContainer.capAmount(-capped_amount);
        if (required_supply != null)
            result = -required_supply.capAmount(-result);
        if (result > 0) {
            if (required_supply != null)
                required_supply.prepareDeploy(result);
            unitContainer.prepareDeploy(result);
            orderSupply(result, orders);
        } else {
            orderSupply(result, orders);
            unitContainer.prepareDeploy(result);
            if (required_supply != null) {
                if (!required_supply.isSupplyFull())
                    required_supply.prepareDeploy(result);
            }
        }
    }

    private void orderSupply(int amount, int orders) {
        increaseSupply(amount);
        num_orders += orders;
    }

    public int getNumOrders() {
        return num_orders;
    }

    public int getMaxSupplyCount() {
        return Integer.MAX_VALUE;
    }

    public void deploy(float amount) {
        time += amount;
        if (time >= seconds_per_deploy) {
            time = 0;
            increaseSupply(-1);
            doDeploy();
        }
    }

    private void doDeploy() {
        switch (deploy_type) {
            case ROCK_WARRIOR -> building.createWarriors(WeaponTier.ROCK, 1);
            case IRON_WARRIOR -> building.createWarriors(WeaponTier.IRON, 1);
            case RUBBER_WARRIOR -> building.createWarriors(WeaponTier.RUBBER, 1);
            case PEON -> building.createPeons(1);
            case PEON_HARVEST_TREE -> building.createHarvesters(SupplyType.WOOD, 1);
            case PEON_TRANSPORT_TREE -> building.createTransporters(SupplyType.WOOD, 1);
            case PEON_HARVEST_ROCK -> building.createHarvesters(SupplyType.ROCK, 1);
            case PEON_TRANSPORT_ROCK -> building.createTransporters(SupplyType.ROCK, 1);
            case PEON_HARVEST_IRON -> building.createHarvesters(SupplyType.IRON, 1);
            case PEON_TRANSPORT_IRON -> building.createTransporters(SupplyType.IRON, 1);
            case PEON_HARVEST_RUBBER -> building.createHarvesters(SupplyType.RUBBER, 1);
            case PEON_TRANSPORT_RUBBER -> building.createTransporters(SupplyType.RUBBER, 1);
        }
    }

    public float getBuildProgress() {
        return time / seconds_per_deploy;
    }
}
