package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.behaviour.IdleController;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

/**
 * Basic artificial intelligence controller for passive or idle units and chieftains.
 */
public final class PassiveAI extends AI {
    private final boolean walk_around;

    public PassiveAI(Player owner, PlayerInterface playerInterface, @Nullable UnitInfo unit_info,
            boolean walk_around) {
        super(owner, playerInterface, unit_info);
        this.walk_around = walk_around;
    }

    @Override
    public void tick() {
        if (walk_around) {
            if (!shouldDoAction())
                return;
            for (Selectable<?> s : getOwner().getUnits().getSet()) {
                if (s.getPrimaryController() instanceof IdleController) {
                    var random = AppConfig.LEGACY_DETERMINISTIC
                            ? getOwner().getWorld().getRandom()
                            : getAIRandom();
                    float r = random.nextFloat();
                    if (r < .2f) {
                        Target walkable_target = getTarget(random);
                        getPlayerInterface().setTarget(s, walkable_target, Action.ATTACK,
                                true);
                    }
                }
            }
            getOwner().getChieftain().ifPresent(chieftain -> getOwner().getRaceInfo().getChieftainAI().decide(
                    chieftain, getPlayerInterface()));
        }
    }
}
