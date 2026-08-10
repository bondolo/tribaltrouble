package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.BuildingType;

import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class SelectTowerTrigger extends TutorialTrigger {
    private final @Nullable Building tower = null;

    public SelectTowerTrigger(@NonNull Player player) {
        super(.1f, 0f, "select_tower");
        player.enableRepairing(false);
        player.enableAttacking(false);
        player.enableBuilding(BuildingType.QUARTERS, false);
        player.enableBuilding(BuildingType.ARMORY, false);
        //	player.enableTower(false);
        player.enableHarvesting(false);
        player.enableWeapons(false);
        player.enableArmies(false);
        player.enableTransporting(false);
        player.enableRallyPoints(false);
        player.enableChieftains(false);
        player.enableTowerExits(false);
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        tutorial.getViewer().getSelection().getCurrentSelection().getBuilding().ifPresent(building -> {
            if (building.getAbilities().hasAbilities(Abilities.ATTACK)) {
                tutorial.next(new UnitInTowerTrigger(building));
            }
        });
    }
}
