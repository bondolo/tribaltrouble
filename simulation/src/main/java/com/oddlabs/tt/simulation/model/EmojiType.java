package com.oddlabs.tt.simulation.model;


/**
 * Logical identifiers for emoji graphics displayed as thought bubbles or visual sounds.
 */
public enum EmojiType {
    GRAVESTONE,
    CHICKEN_CLUCK,
    HARVEST_WOOD,
    HARVEST_ROCK,
    HARVEST_IRON,
    HARVEST_RUBBER,
    REPAIR_SAW,
    REPAIR_HAMMER;

    /**
     * Map a SupplyType to its corresponding harvest EmojiType.
     *
     * @param supplyType The type of supply harvested.
     * @return The corresponding EmojiType.
     */
    public static EmojiType fromSupply(SupplyType supplyType) {
        return switch (supplyType) {
            case WOOD -> HARVEST_WOOD;
            case ROCK -> HARVEST_ROCK;
            case IRON -> HARVEST_IRON;
            case RUBBER -> HARVEST_RUBBER;
        };
    }
}
