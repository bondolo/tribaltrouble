package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.NonNull;

import java.util.Map;

public class BuildProductionContainer extends BuildSupplyContainer {
    public static final int INFINITE_LIMIT = 30;

    private final @NonNull SupplyContainer dest_container;
    private final @NonNull Building building;
    private final @NonNull Cost cost;
    private final float man_seconds_per_production;

    private float man_seconds = 0;
    private boolean infinite = false;

    public BuildProductionContainer(int max_supply_count,
            @NonNull SupplyContainer dest_container,
            @NonNull Building building,
            @NonNull Cost cost,
            float man_seconds_per_production) {
        super(max_supply_count);
        this.dest_container = dest_container;
        this.building = building;
        this.cost = cost;
        this.man_seconds_per_production = man_seconds_per_production;
    }

    public void orderSupply(int amount, boolean infinite) {
        this.infinite = infinite;
        if (infinite)
            super.orderSupply(INFINITE_LIMIT - getNumSupplies(), amount);
        else
            super.orderSupply(amount);
    }

    public final boolean hasEnoughSupplies() {
        for (Map.Entry<@NonNull SupplyType, @NonNull Integer> entry : cost.costs().entrySet()) {
            if (building.getSupplyContainer(entry.getKey()).orElseThrow().getNumSupplies() < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public final void build(float delta) {
        man_seconds += delta;
        if (man_seconds >= man_seconds_per_production) {
            man_seconds = 0;
            if (!dest_container.isSupplyFull()) {
                for (Map.Entry<@NonNull SupplyType, @NonNull Integer> entry : cost.costs().entrySet()) {
                    building.getSupplyContainer(entry.getKey()).orElseThrow().increaseSupply(-entry.getValue());
                }
                if (!infinite)
                    increaseSupply(-1);
                dest_container.increaseSupply(1);
            } else {
                stopProduction();
            }
        }
    }

    public final float getBuildProgress() {
        return man_seconds / man_seconds_per_production;
    }

    private void stopProduction() {
        increaseSupply(-getNumSupplies());
    }
}
