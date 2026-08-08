package com.oddlabs.tt.simulation.tutorial.trigger;

import com.oddlabs.tt.simulation.tutorial.Tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.Building;
import org.jspecify.annotations.NonNull;

public final class EmptyTowerTrigger extends TutorialTrigger {
    private final @NonNull Building tower;

    public EmptyTowerTrigger(@NonNull Building tower) {
        super(.1f, 0f, "empty_tower");
        this.tower = tower;
        tower.getOwner().enableTowerExits(true);
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (tower.getUnitContainer().map(c -> c.getNumSupplies() == 0).orElse(true)) {
            tutorial.done(TutorialForm.TUTORIAL_TOWER);
        }
    }
}
