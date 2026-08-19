package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.NonNull;

public final class ArmyMenuTrigger extends TutorialTrigger {
    public ArmyMenuTrigger(@NonNull Player local_player) {
        super(.1f, 1f, "army_menu");
        local_player.enableArmies(true);
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getPanel().inArmyMenu())
            tutorial.next(new ArmyTrigger(tutorial.getViewer().getLocalPlayer()));
    }
}
