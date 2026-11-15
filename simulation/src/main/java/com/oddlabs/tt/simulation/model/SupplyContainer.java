package com.oddlabs.tt.simulation.model;

public class SupplyContainer {
    private final int max_supply_count;

    private int supply_count = 0;
    private int num_preparing = 0;

    public SupplyContainer(int max_supply_count) {
        this.max_supply_count = max_supply_count;
    }

    public int increaseSupply(int amount) {
        int capped_amount = capAmount(amount);
        supply_count += capped_amount;
        return capped_amount;
    }

    public int capAmount(int amount) {
        int result = supply_count + amount;
        if (amount > 0 && result < supply_count) {
            result = max_supply_count;
        } else if (amount < 0 && result > supply_count) {
            result = 0;
        } else {
            result = Math.clamp(result, 0, max_supply_count);
        }
        return result - supply_count;
    }

    public final void prepareDeploy(int amount) {
        int result = increaseSupply(-amount);
        assert result == -amount;
        num_preparing += amount;
    }

    public final boolean isSupplyFull() {
        return supply_count == max_supply_count;
    }

    public final int getNumSupplies() {
        return supply_count;
    }

    public final int getNumPreparing() {
        return num_preparing;
    }

    public final int getMaxSupplyCount() {
        return max_supply_count;
    }
}
