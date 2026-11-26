package com.oddlabs.tt.content.tutorial.trigger;

import static com.oddlabs.tt.simulation.model.Ability.MAGIC;

import com.oddlabs.tt.content.tutorial.Tutorial;
import com.oddlabs.tt.simulation.model.Unit;

/// Tutorial trigger waiting for a chieftain unit to be completed.
final class ChieftainBuiltTrigger extends TutorialTrigger {

  ChieftainBuiltTrigger() {
    super(.1f, 0f, "chieftain_built");
  }

  @Override
  public void run(Tutorial tutorial) {
    for (var s : tutorial.getViewer().getLocalPlayer().getUnits().getSet()) {
      if (s instanceof Unit u && u.hasAbility(MAGIC)) {
        tutorial.next(new MagicTrigger(u));
      }
    }
  }
}
