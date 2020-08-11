package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.Unit;

public final strictfp class UnitCountTrigger extends TutorialTrigger {

    private final int target_count;

    public UnitCountTrigger(int target_count) {
        super(1f, 0f, "unit_count", new Object[]{target_count});
        this.target_count = target_count;
    }

    @Override
    protected void run(Tutorial tutorial) {
        long count = tutorial.getViewer().getLocalPlayer().getUnits().getSet().stream()
                .filter(s -> s instanceof Unit)
                .count();

        if (count >= target_count)
            tutorial.done(TutorialForm.TutorialType.TUTORIAL_QUARTERS);
    }
}
