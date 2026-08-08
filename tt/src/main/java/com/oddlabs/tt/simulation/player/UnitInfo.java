package com.oddlabs.tt.simulation.player;

public record UnitInfo(boolean hasQuarters,
                       boolean hasArmory,
                       int numTowers,
                       boolean hasChieftain,
                       int numPeons,
                       int numRockWarriors,
                       int numIronWarriors,
                       int numRubberWarriors) {
    public UnitInfo() {
        this(false, false, 0, false, 0, 0, 0, 0);
    }
}
