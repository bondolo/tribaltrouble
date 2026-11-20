package com.oddlabs.tt.ai;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.EntityActivity;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.player.Player;

/// Basic artificial intelligence controller for passive or idle units and chieftains.
public final class PassiveAI extends AI {
  private final boolean walk_around;

  public PassiveAI(Player owner, boolean walk_around) {
    super(owner);
    this.walk_around = walk_around;
  }

  @Override
  public void tick() {
    if (walk_around) {
      if (!shouldDoAction()) return;
      for (Selectable<?> s : getOwner().getUnits().getSet()) {
        if (s.getActivity() == EntityActivity.IDLE) {
          var random = AppConfig.LEGACY_DETERMINISTIC ? getOwner().getWorld().getRandom() : getAIRandom();
          float r = random.nextFloat();
          if (r < .2f) {
            Target walkable_target = getTarget(random);
            getPlayerInterface()
                .setLandscapeTarget(s, walkable_target.getGridX(), walkable_target.getGridY(), Action.ATTACK, true);
          }
        }
      }
      getOwner()
          .getChieftain()
          .ifPresent(chieftain ->
              ChieftainAI.forRace(getOwner().getPlayerInfo().race()).decide(chieftain, getPlayerInterface()));
    }
  }
}
