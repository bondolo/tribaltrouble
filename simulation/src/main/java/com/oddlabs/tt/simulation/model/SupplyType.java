package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.TreeSupply;
import org.jspecify.annotations.Nullable;

/**
 * Logical identifiers for resource supply types in the simulation.
 */
public enum SupplyType {
    WOOD(TreeSupply.class),
    ROCK(RockSupply.class),
    IRON(IronSupply.class),
    RUBBER(RubberSupply.class);

    private final Class<? extends Supply> supplyClass;

    SupplyType(Class<? extends Supply> supplyClass) {
        this.supplyClass = supplyClass;
    }

    public Class<? extends Supply> getSupplyClass() {
        return supplyClass;
    }

    public static @Nullable SupplyType fromClass(Class<?> cl) {
        if (cl == TreeSupply.class) {
            return WOOD;
        } else if (cl == RockSupply.class) {
            return ROCK;
        } else if (cl == IronSupply.class) {
            return IRON;
        } else if (cl == RubberSupply.class) {
            return RUBBER;
        }
        return null;
    }
}
