package com.oddlabs.tt.content.skirmish;

import com.oddlabs.tt.simulation.model.Race;

/**
 * Represents a player slot configuration in a skirmish scenario match.
 */
public record SkirmishPlayerSlot(
                                 int slotIndex,
                                 boolean isHuman,
                                 Race race,
                                 int team,
                                 int aiDifficulty
) {
}
