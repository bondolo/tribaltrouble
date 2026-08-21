package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.player.Player;

public final class HarvestMenuTrigger extends TutorialTrigger {
    public HarvestMenuTrigger(Player local_player) {
        super(.1f, 0f, "harvest_menu");
        local_player.enableHarvesting(true);
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tutorial.getViewer().getPanel().inHarvestMenu())
            tutorial.next(new SupplyTrigger(tutorial.getViewer().getLocalPlayer()));
    }
}
