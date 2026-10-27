package com.oddlabs.tt.simulation.model;

/**
 * Logical identifiers for resource supply types in the simulation.
 */
public enum SupplyType {
    WOOD,
    ROCK,
    IRON,
    RUBBER;

    private static final SupplyType[] VALUES = values();

    /** {@return array of all supply types without allocating a new array} */
    public static SupplyType[] getValues() {
        return VALUES;
    }
}
