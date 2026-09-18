package com.oddlabs.tt.simulation.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Captures the supply types and amounts needed for a production.
 */
public record Cost(Map<SupplyType, Integer> costs, int[] amounts) {

    /**
     * Functional consumer for iterating supply costs without allocation.
     */
    @FunctionalInterface
    public interface CostConsumer {
        /**
         * Consumes a supply type and required amount.
         *
         * @param type supply type
         * @param amount required quantity
         */
        void accept(SupplyType type, int amount);
    }

    /**
     * Constructs a Cost instance with the specified map of required supplies.
     *
     * @param costs map of supply types to amounts
     */
    public Cost(Map<SupplyType, Integer> costs) {
        this(new EnumMap<>(costs), createAmounts(costs));
    }

    private static int[] createAmounts(Map<SupplyType, Integer> map) {
        SupplyType[] types = SupplyType.getValues();
        int[] arr = new int[types.length];
        for (SupplyType type : types) {
            arr[type.ordinal()] = map.getOrDefault(type, 0);
        }
        return arr;
    }

    public int getCost(SupplyType supplyType) {
        return amounts[supplyType.ordinal()];
    }

    /**
     * Executes the given action for each supply type with a non-zero required cost without allocating iterators.
     *
     * @param action consumer for supply types and amounts
     */
    public void forEachCost(CostConsumer action) {
        SupplyType[] types = SupplyType.getValues();
        for (SupplyType type : types) {
            int amount = amounts[type.ordinal()];
            if (amount > 0) {
                action.accept(type, amount);
            }
        }
    }
}
