package com.oddlabs.tt.simulation.tutorial.trigger;

import com.oddlabs.tt.simulation.tutorial.Tutorial;

import com.oddlabs.tt.model.Building;
import org.jspecify.annotations.NonNull;

public final class QuartersTrigger extends TutorialTrigger {
    public QuartersTrigger() {
        super(1f, 0f, "quarters");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        var set = tutorial.getViewer().getLocalPlayer().getUnits().getSet();
        for (var s : set) {
            if (s instanceof Building)
                tutorial.next(new SelectQuartersTrigger());
        }
    }
}
