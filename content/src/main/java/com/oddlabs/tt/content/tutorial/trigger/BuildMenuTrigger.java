package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.player.Player;

public final class BuildMenuTrigger extends TutorialTrigger {
    public BuildMenuTrigger(Player local_player) {
        super(.1f, 0f, "build_menu");
        local_player.enableWeapons(true);
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tutorial.getViewer().getPanel().inBuildMenu())
            tutorial.next(new WeaponTrigger(tutorial.getViewer().getLocalPlayer()));
    }
}
