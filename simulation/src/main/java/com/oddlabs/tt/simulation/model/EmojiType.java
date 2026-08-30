package com.oddlabs.tt.simulation.model;


/**
 * Logical identifiers for emoji graphics displayed as thought bubbles or visual sounds.
 */
public enum EmojiType {
    GRAVESTONE(1.5f),
    CHICKEN_CLUCK(0.8f),
    HARVEST_WOOD(1.0f),
    HARVEST_ROCK(1.0f),
    HARVEST_IRON(1.0f),
    HARVEST_RUBBER(1.0f),
    REPAIR_SAW(1.0f),
    REPAIR_HAMMER(1.0f);

    private final float duration;

    EmojiType(float duration) {
        this.duration = duration;
    }

    /**
     * The standard display duration for this emoji in seconds.
     *
     * @return the display duration in seconds
     */
    public float getDuration() {
        return duration;
    }

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
