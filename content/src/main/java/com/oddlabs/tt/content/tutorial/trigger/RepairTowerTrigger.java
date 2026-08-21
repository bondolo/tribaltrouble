package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Building;

public final class RepairTowerTrigger extends TutorialTrigger {
    private final Building tower;

    public RepairTowerTrigger(Building tower) {
        super(.1f, 0f, "repair");
        this.tower = tower;
        tower.getOwner().enableRepairing(true);
    }

    @Override
    public void run(Tutorial tutorial) {
        if (!tower.isDamaged()) {
            tutorial.next(new EmptyTowerTrigger(tower));
        }
    }
}
