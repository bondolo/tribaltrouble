package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import org.jspecify.annotations.NonNull;

public final class SelectUnitTrigger extends TutorialTrigger {
    public SelectUnitTrigger() {
        super(.1f, 15f, "select_unit");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getSelection().getCurrentSelection().getNumUnits() > 0)
            tutorial.next(new MoveUnitTrigger(tutorial.getViewer().getLocalPlayer()));
    }
}
