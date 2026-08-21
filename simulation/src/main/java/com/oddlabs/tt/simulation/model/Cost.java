package com.oddlabs.tt.simulation.model;


import java.util.EnumMap;
import java.util.Map;

/** Captures the supply types and amounts needed for a production. */
public record Cost(Map<SupplyType, Integer> costs) {

    public Cost {
        costs = new EnumMap<>(costs);
    }

    public int getCost(SupplyType supplyType) {
        return costs.getOrDefault(supplyType, 0);
    }
}
