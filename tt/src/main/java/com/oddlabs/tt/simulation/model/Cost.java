package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.Map;

/** Captures the supply types and amounts needed for a production. */
public record Cost(@NonNull Map<@NonNull SupplyType, @NonNull Integer> costs) {

    public Cost {
        costs = new EnumMap<>(costs);
    }

    public int getCost(@NonNull SupplyType supplyType) {
        return costs.getOrDefault(supplyType, 0);
    }
}
