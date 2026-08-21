package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Building;

public final class UnitInTowerTrigger extends TutorialTrigger {
    private final Building tower;

    public UnitInTowerTrigger(Building tower) {
        super(.1f, 0f, "unit_in_tower");
        this.tower = tower;
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tower.getUnitContainer().map(c -> c.getNumSupplies() > 0).orElse(false)) {
            tutorial.next(new AttackTowerTrigger(tower));
        }
    }
}
