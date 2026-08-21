package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.player.Player;

public final class WeaponTrigger extends TutorialTrigger {
    private static final int WEAPONS = 10;

    public WeaponTrigger(Player local_player) {
        super(.5f, 0f, "weapon", new Object[]{WEAPONS});
        local_player.enableHarvesting(true);
    }

    @Override
    public void run(Tutorial tutorial) {
        for (var s : tutorial.getViewer().getSelection().getCurrentSelection().getSet()) {
            if (s instanceof Building armory && s.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
                if (armory.getSupplyContainer(com.oddlabs.tt.simulation.model.weapon.RockAxeWeapon.class)
                        .map(c -> c.getNumSupplies()).orElse(0) >= WEAPONS)
                    tutorial.next(new ArmyMenuTrigger(tutorial.getViewer().getLocalPlayer()));
            }
        }
    }
}
