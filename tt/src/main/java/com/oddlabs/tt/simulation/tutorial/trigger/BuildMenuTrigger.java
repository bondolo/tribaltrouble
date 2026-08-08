package com.oddlabs.tt.simulation.tutorial.trigger;

import com.oddlabs.tt.simulation.tutorial.Tutorial;

import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.NonNull;

public final class BuildMenuTrigger extends TutorialTrigger {
    public BuildMenuTrigger(@NonNull Player local_player) {
        super(.1f, 0f, "build_menu");
        local_player.enableWeapons(true);
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getPanel().inBuildMenu())
            tutorial.next(new WeaponTrigger(tutorial.getViewer().getLocalPlayer()));
    }
}
