package com.oddlabs.tt.simulation.model;

import com.oddlabs.util.Color;

/**
 * Landscape terrain types.
 */
public enum Terrain {
    NATIVE(0),
    VIKING(1);

    private static final Color.Linear NATIVE_SAND_COLOR = new Color.Standard(0xFF_FF_E6_CC).linear();
    private static final Color.Linear VIKING_SOIL_COLOR = new Color.Standard(0xFF_A6_80_59).linear();

    private final int value;

    Terrain(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Returns the baseline dust colour for this terrain type.
     *
     * @return the linear colour representing the terrain dust/soil
     */
    public Color.Linear getDustColor() {
        return switch (this) {
            case NATIVE -> NATIVE_SAND_COLOR;
            case VIKING -> VIKING_SOIL_COLOR;
        };
    }

    public static Terrain fromValue(int value) {
        for (Terrain type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown terrain value: " + value);
    }
}
