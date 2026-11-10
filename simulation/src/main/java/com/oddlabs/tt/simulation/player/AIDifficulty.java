package com.oddlabs.tt.simulation.player;

import org.jspecify.annotations.Nullable;

/// Artificial intelligence personality and difficulty profiles for bots and campaign scenarios.
public enum AIDifficulty {
    NONE(0),
    EASY(1),
    NORMAL(2),
    HARD(3),
    TOWER_TUTORIAL(4),
    CHIEFTAIN_TUTORIAL(5),
    BATTLE_TUTORIAL(6),
    PASSIVE_CAMPAIGN(7),
    NEUTRAL_CAMPAIGN(8);

    private final int value;

    AIDifficulty(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AIDifficulty fromValue(int value) {
        return switch (value) {
            case 0 -> NONE;
            case 1 -> EASY;
            case 2 -> NORMAL;
            case 3 -> HARD;
            case 4 -> TOWER_TUTORIAL;
            case 5 -> CHIEFTAIN_TUTORIAL;
            case 6 -> BATTLE_TUTORIAL;
            case 7 -> PASSIVE_CAMPAIGN;
            case 8 -> NEUTRAL_CAMPAIGN;
            default -> throw new IllegalArgumentException("Unknown AIDifficulty value: " + value);
        };
    }

    public static @Nullable AIDifficulty fromValueOrNull(int value) {
        return switch (value) {
            case 0 -> NONE;
            case 1 -> EASY;
            case 2 -> NORMAL;
            case 3 -> HARD;
            case 4 -> TOWER_TUTORIAL;
            case 5 -> CHIEFTAIN_TUTORIAL;
            case 6 -> BATTLE_TUTORIAL;
            case 7 -> PASSIVE_CAMPAIGN;
            case 8 -> NEUTRAL_CAMPAIGN;
            default -> null;
        };
    }
}
