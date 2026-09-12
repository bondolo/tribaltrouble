package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.player.Player;

/** Tutorial trigger waiting for the player to open the army menu. */
final class ArmyMenuTrigger extends TutorialTrigger {
    ArmyMenuTrigger(Player local_player) {
        super(.1f, 1f, "army_menu");
        local_player.enableArmies(true);
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tutorial.getViewer().getPanel().inArmyMenu())
            tutorial.next(new ArmyTrigger(tutorial.getViewer().getLocalPlayer()));
    }
}
