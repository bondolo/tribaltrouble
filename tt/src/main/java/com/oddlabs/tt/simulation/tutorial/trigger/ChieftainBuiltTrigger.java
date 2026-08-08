package com.oddlabs.tt.simulation.tutorial.trigger;

import com.oddlabs.tt.simulation.tutorial.Tutorial;

import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Unit;
import org.jspecify.annotations.NonNull;

public final class ChieftainBuiltTrigger extends TutorialTrigger {

    public ChieftainBuiltTrigger() {
        super(.1f, 0f, "chieftain_built");
    }

    @Override
    public void run(@NonNull Tutorial tutorial) {
        for (var s : tutorial.getViewer().getLocalPlayer().getUnits().getSet()) {
            if (s instanceof Unit u && u.getAbilities().hasAbilities(Abilities.MAGIC)) {
                tutorial.next(new MagicTrigger(u));
            }
        }
    }
}
