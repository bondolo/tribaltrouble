package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Abilities;

/** Tutorial trigger waiting for the player to select a quarters building. */
final class SelectQuartersTrigger extends TutorialTrigger {
    SelectQuartersTrigger() {
        super(.1f, 0f, "select_quarters");
    }

    @Override
    public void run(Tutorial tutorial) {
        tutorial.getViewer().getSelection().getCurrentSelection().getBuilding().ifPresent(building -> {
            if (building.getAbilities().hasAbilities(Abilities.REPRODUCE))
                tutorial.next(new UnitsInQuartersTrigger());
        });
    }
}
