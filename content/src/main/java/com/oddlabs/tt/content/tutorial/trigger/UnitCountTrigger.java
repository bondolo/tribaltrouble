package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.content.tutorial.TutorialForm;
import com.oddlabs.tt.simulation.model.Unit;
import org.jspecify.annotations.NonNull;

public final class UnitCountTrigger extends TutorialTrigger {
    private final int target_count;

    public UnitCountTrigger(int target_count) {
        super(1f, 0f, "unit_count", new Object[]{target_count});
        this.target_count = target_count;
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        var set = tutorial.getViewer().getLocalPlayer().getUnits().getSet();
        var it = set.iterator();
        int count = 0;
        while (it.hasNext()) {
            var s = it.next();
            if (s instanceof Unit)
                count++;
        }
        if (count >= target_count)
            tutorial.done(TutorialForm.TUTORIAL_QUARTERS);
    }
}
