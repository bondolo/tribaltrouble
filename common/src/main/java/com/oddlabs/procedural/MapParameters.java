package com.oddlabs.procedural;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Procedural island configuration and player slot parameters encoded in a map code.
 *
 * @param seed procedural terrain generation seed
 * @param hills hilliness slider factor (0-10)
 * @param vegetation vegetation slider factor (0-10)
 * @param supplies supply resources slider factor (0-10)
 * @param terrainType terrain type identifier (0: Native, 1: Viking)
 * @param size size index (0: small, 1: medium, 2: large)
 * @param player0Race race ordinal of player 0
 * @param player0Team team identifier of player 0
 * @param otherSlots slot settings for players 1 through 5
 */
@NullMarked
public record MapParameters(
                            int seed,
                            int hills,
                            int vegetation,
                            int supplies,
                            int terrainType,
                            int size,
                            int player0Race,
                            int player0Team,
                            List<SlotSetting> otherSlots) {

    public MapParameters {
        otherSlots = List.copyOf(otherSlots);
    }

    /**
     * Configuration setting for an additional player slot (slots 1 to 5).
     *
     * @param difficulty difficulty tier index (0: closed, 1: easy, 2: normal, 3: hard)
     * @param race race ordinal (0: Viking, 1: Native)
     * @param team team identifier (0 to 5)
     */
    public record SlotSetting(int difficulty, int race, int team) {
    }
}
