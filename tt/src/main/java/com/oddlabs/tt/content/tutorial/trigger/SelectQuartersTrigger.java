package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Abilities;
import org.jspecify.annotations.NonNull;

public final class SelectQuartersTrigger extends TutorialTrigger {
    public SelectQuartersTrigger() {
        super(.1f, 0f, "select_quarters");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        tutorial.getViewer().getSelection().getCurrentSelection().getBuilding().ifPresent(building -> {
            if (building.getAbilities().hasAbilities(Abilities.REPRODUCE))
                tutorial.next(new UnitsInQuartersTrigger());
        });
    }
}
