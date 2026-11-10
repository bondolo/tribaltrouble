package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.Nullable;


/**
 * Playable races in the game.
 */
public enum Race {
    NATIVES(0),
    VIKINGS(1);

    private final int value;

    Race(int value) {
        this.value = value;
    }

    /**
     * Gets the legacy integer value associated with this race type.
     * Used to maintain compatibility with legacy serialization and network protocols.
     *
     * @return the legacy integer value
     */
    public int getValue() {
        return value;
    }

    /**
     * Resolves the {@link Race} associated with the given legacy integer value.
     *
     * @param value legacy integer value
     * @return the corresponding {@link Race}
     * @throws IllegalArgumentException if the legacy value is unrecognized
     */
    public static Race fromValue(int value) {
        return switch (value) {
            case 0 -> NATIVES;
            case 1 -> VIKINGS;
            default -> throw new IllegalArgumentException("Unknown race value: " + value);
        };
    }

    /**
     * Resolves the {@link Race} associated with the given legacy integer value, or {@code null} if unrecognized.
     *
     * @param value legacy integer value
     * @return the corresponding {@link Race}, or {@code null} if unrecognized
     */
    public static @Nullable Race fromValueOrNull(int value) {
        return switch (value) {
            case 0 -> NATIVES;
            case 1 -> VIKINGS;
            default -> null;
        };
    }
}
