package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.BuildingType;

import com.oddlabs.tt.client.delegate.PlacingDelegate;
import com.oddlabs.tt.simulation.player.Player;

public final class PlacingDelegateTrigger extends TutorialTrigger {
    public PlacingDelegateTrigger(Player player) {
        super(.1f, 0f, "placing");
        player.enableRepairing(false);
        player.enableAttacking(false);
        player.enableBuilding(BuildingType.ARMORY, false);
        player.enableBuilding(BuildingType.TOWER, false);
        player.enableChieftains(false);
    }

    @Override
    public void run(Tutorial tutorial) {
        if (tutorial.getViewer().getGUIRoot().getDelegate() instanceof PlacingDelegate)
            tutorial.next(new QuartersTrigger());
    }
}
