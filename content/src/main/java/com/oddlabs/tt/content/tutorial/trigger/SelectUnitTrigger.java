package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;


/** Tutorial trigger waiting for the player to select a unit. */
final class SelectUnitTrigger extends TutorialTrigger {
    SelectUnitTrigger() {
        super(.1f, 15f, "select_unit");
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tutorial.getViewer().getSelection().getCurrentSelection().getNumUnits() > 0)
            tutorial.next(new MoveUnitTrigger(tutorial.getViewer().getLocalPlayer()));
    }
}
