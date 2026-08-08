package com.oddlabs.tt.simulation.tutorial.trigger;

import com.oddlabs.tt.simulation.tutorial.Tutorial;

import com.oddlabs.tt.client.form.TutorialForm;
import com.oddlabs.tt.model.behaviour.WalkController;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.NonNull;

public final class MoveUnitTrigger extends TutorialTrigger {
    public MoveUnitTrigger(@NonNull Player local_player) {
        super(1f, 2f, "move_unit");
        local_player.enableMoving(true);
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        for (var s : tutorial.getViewer().getSelection().getCurrentSelection().getSet()) {
            if (s.getPrimaryController() instanceof WalkController) {
                tutorial.done(TutorialForm.TUTORIAL_CAMERA);
            }
        }
    }
}
