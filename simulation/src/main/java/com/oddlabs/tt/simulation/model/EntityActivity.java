package com.oddlabs.tt.simulation.model;

/// Observable activity state of a unit or building.
public enum EntityActivity {
    IDLE,
    MOVING,
    ATTACKING_MOVING,
    ACTIVE,
    GATHERING,
    PLACING_BUILDING,
    UNDER_CONSTRUCTION,
    STUNNED,
    DEAD,
    OTHER
}
