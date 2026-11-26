package com.oddlabs.tt.content.tutorial.trigger;

import static com.oddlabs.tt.simulation.model.Ability.ATTACK;

import com.oddlabs.tt.content.tutorial.Tutorial;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.player.Player;

/// Tutorial trigger waiting for the player to select a tower building.
public final class SelectTowerTrigger extends TutorialTrigger {
  public SelectTowerTrigger(Player player) {
    super(.1f, 0f, "select_tower");
    player.enableRepairing(false);
    player.enableAttacking(false);
    player.enableBuilding(BuildingType.QUARTERS, false);
    player.enableBuilding(BuildingType.ARMORY, false);
    player.enableHarvesting(false);
    player.enableWeapons(false);
    player.enableArmies(false);
    player.enableTransporting(false);
    player.enableRallyPoints(false);
    player.enableChieftains(false);
    player.enableTowerExits(false);
  }

  @Override
  public void run(Tutorial tutorial) {
    tutorial
        .getViewer()
        .getSelection()
        .getCurrentSelection()
        .getBuilding()
        .ifPresent(building -> {
          if (building.hasAbility(ATTACK)) {
            tutorial.next(new UnitInTowerTrigger(building));
          }
        });
  }
}
