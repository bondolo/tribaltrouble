package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Building;

/// Building entry state walking to a target building and entering it with supply/weapon deposit.
public final class EnteringBuilding implements UnitState {
    private final Building building;
    private final UnitState fallbackState;
    private final GiveUpTracker.Single giveUpTracker = new GiveUpTracker.Single();

    public EnteringBuilding(Building building, UnitState fallbackState) {
        this.building = building;
        this.fallbackState = fallbackState;
    }

    @Override
    public Transition decide(UnitContext context) {
        if (building.isDead()) {
            return Transition.to(fallbackState);
        }
        if (context.isCloseEnough(0f, building)) {
            if (context.canEnterBuilding(building)) {
                context.enterBuilding(building);
                if (context.isDead()) {
                    return Transition.stay();
                }
            }
            return Transition.to(fallbackState);
        }
        if (giveUpTracker.shouldGiveUp()) {
            return Transition.to(fallbackState);
        }
        context.initWalkTarget(building, 0f);
        return Transition.walk(false);
    }

    @Override
    public UnitState getFallbackState() {
        return fallbackState;
    }

    @Override
    public void resetGiveUpCounters() {
        giveUpTracker.reset();
    }
}
