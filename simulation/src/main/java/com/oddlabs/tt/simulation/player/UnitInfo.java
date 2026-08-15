package com.oddlabs.tt.simulation.player;

import java.io.Serializable;

/** Serializable unit info descriptor. */
public record UnitInfo(boolean hasQuarters,
                       boolean hasArmory,
                       int numTowers,
                       boolean hasChieftain,
                       int numPeons,
                       int numRockWarriors,
                       int numIronWarriors,
                       int numRubberWarriors) implements Serializable {
}
