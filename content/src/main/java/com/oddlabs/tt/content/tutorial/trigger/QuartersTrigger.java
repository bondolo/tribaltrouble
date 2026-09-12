package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Building;

/** Tutorial trigger waiting for quarters building construction to start. */
final class QuartersTrigger extends TutorialTrigger {
    QuartersTrigger() {
        super(1f, 0f, "quarters");
    }

    @Override
    public void run(Tutorial tutorial) {
        var set = tutorial.getViewer().getLocalPlayer().getUnits().getSet();
        for (var s : set) {
            if (s instanceof Building)
                tutorial.next(new SelectQuartersTrigger());
        }
    }
}
