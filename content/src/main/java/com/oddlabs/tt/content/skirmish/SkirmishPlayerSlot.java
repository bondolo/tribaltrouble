package com.oddlabs.tt.content.skirmish;

import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.player.AIDifficulty;

/// Represents a player slot configuration in a skirmish scenario match.
public record SkirmishPlayerSlot(int slotIndex, boolean isHuman, Race race, int team, AIDifficulty aiDifficulty) {}
