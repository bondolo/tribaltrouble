package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.BuildingType;

import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.player.Player;

public final class SelectArmoryTrigger extends TutorialTrigger {
    public SelectArmoryTrigger(Player player) {
        super(.1f, 0f, "select_armory");
        player.enableRepairing(false);
        player.enableAttacking(false);
        player.enableBuilding(BuildingType.QUARTERS, false);
        player.enableBuilding(BuildingType.TOWER, false);
        player.enableHarvesting(false);
        player.enableWeapons(false);
        player.enableArmies(false);
        player.enableTransporting(false);
        player.enableRallyPoints(false);
        player.enableChieftains(false);
    }

    @Override
    public void run(Tutorial tutorial) {
        tutorial.getViewer().getSelection().getCurrentSelection().getBuilding().ifPresent(building -> {
            if (building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES))
                tutorial.next(new HarvestMenuTrigger(tutorial.getViewer().getLocalPlayer()));
        });
    }
}
