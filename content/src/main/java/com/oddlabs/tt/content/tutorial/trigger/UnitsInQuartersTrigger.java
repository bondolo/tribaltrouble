package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Unit;

public final class UnitsInQuartersTrigger extends TutorialTrigger {
    public UnitsInQuartersTrigger() {
        super(1f, 0f, "units_in_quarters");
    }

    @Override
    public void run(Tutorial tutorial) {
        var set = tutorial.getViewer().getLocalPlayer().getUnits().getSet();
        var it = set.iterator();
        int count = 0;
        while (it.hasNext()) {
            var s = it.next();
            if (s instanceof Unit)
                count++;
        }
        if (count == 0)
            tutorial.next(new RallyPointTrigger());
    }
}
