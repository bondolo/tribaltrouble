package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.content.tutorial.TutorialForm;
import com.oddlabs.tt.simulation.model.Building;

public final class EmptyTowerTrigger extends TutorialTrigger {
    private final Building tower;

    public EmptyTowerTrigger(Building tower) {
        super(.1f, 0f, "empty_tower");
        this.tower = tower;
        tower.getOwner().enableTowerExits(true);
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tower.getUnitContainer().map(c -> c.getNumSupplies() == 0).orElse(true)) {
            tutorial.done(TutorialForm.TUTORIAL_TOWER);
        }
    }
}
