package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Building;
import org.jspecify.annotations.NonNull;

public final class RallyPointTrigger extends TutorialTrigger {
    public RallyPointTrigger() {
        super(1f, 0f, "rally_point");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        for (var s : tutorial.getViewer().getSelection().getCurrentSelection().getSet()) {
            if (s instanceof Building b) {
                if (b.hasRallyPoint())
                    tutorial.next(new UnitCountTrigger(30));
            }
        }
    }
}
