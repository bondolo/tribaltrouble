package com.oddlabs.tt.simulation.tutorial.trigger;

import com.oddlabs.tt.simulation.tutorial.Tutorial;

import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.NonNull;

public final class HarvestMenuTrigger extends TutorialTrigger {
    public HarvestMenuTrigger(@NonNull Player local_player) {
        super(.1f, 0f, "harvest_menu");
        local_player.enableHarvesting(true);
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        if (tutorial.getViewer().getPanel().inHarvestMenu())
            tutorial.next(new SupplyTrigger(tutorial.getViewer().getLocalPlayer()));
    }
}
