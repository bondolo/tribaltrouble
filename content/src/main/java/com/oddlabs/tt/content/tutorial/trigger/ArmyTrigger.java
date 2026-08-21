package com.oddlabs.tt.content.tutorial.trigger;

import com.oddlabs.tt.content.tutorial.Tutorial;

import com.oddlabs.tt.content.tutorial.TutorialForm;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.Player;

public final class ArmyTrigger extends TutorialTrigger {
    private static final int ARMY_SIZE = 10;

    public ArmyTrigger(Player local_player) {
        super(1f, 0f, "army", new Object[]{ARMY_SIZE});
        local_player.enableMoving(true);
    }

    @Override
    public void run(Tutorial tutorial) {
        var count = tutorial.getViewer().getLocalPlayer().getUnits().getSet().stream()
                .filter(s -> s instanceof Unit && s.getAbilities().hasAbilities(Abilities.THROW))
                .limit(ARMY_SIZE)
                .count();

        if (count >= ARMY_SIZE)
            tutorial.done(TutorialForm.TUTORIAL_ARMORY);
    }
}
