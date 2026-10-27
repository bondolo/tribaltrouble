package com.oddlabs.tt.simulation.model;

/**
 * Manages production and resource consumption for manufactured goods in a building.
 */
public final class BuildProductionContainer extends SupplyContainer {
    public static final int INFINITE_LIMIT = 30;

    private final SupplyContainer dest_container;
    private final Building building;
    private final Cost cost;
    private final float man_seconds_per_production;

    private float man_seconds = 0;
    private boolean infinite = false;
    private int num_orders = 0;

    BuildProductionContainer(int max_supply_count,
            SupplyContainer dest_container,
            Building building,
            Cost cost,
            float man_seconds_per_production) {
        super(max_supply_count);
        this.dest_container = dest_container;
        this.building = building;
        this.cost = cost;
        this.man_seconds_per_production = man_seconds_per_production;
    }

    public void orderSupply(int amount) {
        orderSupply(amount, amount);
    }

    private void orderSupply(int amount, int orders) {
        increaseSupply(amount);
        num_orders += orders;
    }

    public int getNumOrders() {
        return num_orders;
    }

    public void orderSupply(int amount, boolean infinite) {
        this.infinite = infinite;
        if (infinite)
            orderSupply(INFINITE_LIMIT - getNumSupplies(), amount);
        else
            orderSupply(amount);
    }

    public boolean hasEnoughSupplies() {
        SupplyType[] types = SupplyType.getValues();
        for (SupplyType type : types) {
            int needed = cost.getCost(type);
            if (needed > 0) {
                SupplyContainer sc = building.getSupplyContainerDirect(type);
                if (sc == null || sc.getNumSupplies() < needed) {
                    return false;
                }
            }
        }
        return true;
    }

    public void build(float delta) {
        man_seconds += delta;
        if (man_seconds >= man_seconds_per_production) {
            man_seconds = 0;
            if (!dest_container.isSupplyFull()) {
                cost.forEachCost((type, amount) -> {
                    SupplyContainer sc = building.getSupplyContainerDirect(type);
                    if (sc != null) {
                        sc.increaseSupply(-amount);
                    }
                });
                if (!infinite)
                    increaseSupply(-1);
                dest_container.increaseSupply(1);
            } else {
                stopProduction();
            }
        }
    }

    public float getBuildProgress() {
        return man_seconds / man_seconds_per_production;
    }

    private void stopProduction() {
        increaseSupply(-getNumSupplies());
    }
}
