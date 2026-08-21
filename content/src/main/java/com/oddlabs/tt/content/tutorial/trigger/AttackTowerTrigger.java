package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.UnitType;

import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.Player;

/**
 * Tutorial trigger that commands the AI player to attack the player's tower
 * to demonstrate battle mechanics and defense.
 */
public final class AttackTowerTrigger extends TutorialTrigger {
    private static final int NUM_UNITS = 12;

    private final Building tower;
    private final Player ai;

    public AttackTowerTrigger(Building tower) {
        super(.1f, 0f, "attack_tower");
        this.ai = tower.getOwner().getWorld().getPlayers().get(1);
        this.tower = tower;
        Selectable<?>[] units = Selectable.newArray(NUM_UNITS);
        for (int i = 0; i < units.length; i++) {
            units[i] = new Unit(ai, tower.getPositionX() - 50, tower.getPositionY() - 50, null, ai.getRaceInfo()
                    .getUnitTemplate(UnitType.WARRIOR_ROCK));
        }
        ai.setTarget(units, tower, Action.ATTACK, false);
    }

    @Override
    public void run(Tutorial tutorial) {
        if (ai.getUnitCountContainer().getNumSupplies() == 0) {
            tutorial.next(new RepairTowerTrigger(tower));
        }
    }
}
