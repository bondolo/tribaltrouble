package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.content.tutorial.TutorialForm;
import com.oddlabs.tt.simulation.model.EntityActivity;
import com.oddlabs.tt.simulation.player.Player;

/** Tutorial trigger waiting for the player to issue a move command to a unit. */
final class MoveUnitTrigger extends TutorialTrigger {
    MoveUnitTrigger(Player local_player) {
        super(1f, 2f, "move_unit");
        local_player.enableMoving(true);
    }

    @Override
    public void run(Tutorial tutorial) {
        for (var s : tutorial.getViewer().getSelection().getCurrentSelection().getSet()) {
            if (s.getActivity() == EntityActivity.MOVING || s.getActivity() == EntityActivity.ATTACKING_MOVING) {
                tutorial.done(TutorialForm.TUTORIAL_CAMERA);
            }
        }
    }
}
