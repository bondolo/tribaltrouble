package com.oddlabs.tt.simulation.trigger;

import com.oddlabs.tt.simulation.behaviour.NullController;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyContainer;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.player.Player;

/**
 * Triggers when a player has gathered a specified amount of a supply type.
 */
public final class SupplyGatheredTrigger extends IntervalTrigger {
    private final Runnable runnable;
    private final SupplyType type;
    private final int goal;
    private final Player local_player;

    public SupplyGatheredTrigger(Player local_player, Runnable runnable, SupplyType type,
            int goal) {
        super(local_player.getWorld(), .5f, 0f);
        this.local_player = local_player;
        this.runnable = runnable;
        this.type = type;
        this.goal = goal;
    }

    @Override
    protected void check() {
        int count = 0;
        for (Selectable<?> s : local_player.getUnits().getSet()) {
            if (s instanceof Building building
                    && building.getPrimaryController() instanceof NullController
                    && building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
                count += building.getSupplyContainer(type)
                        .map(SupplyContainer::getNumSupplies)
                        .orElse(0);
            }
        }

        if (count >= goal)
            triggered();
    }

    @Override
    protected void done() {
        runnable.run();
    }
}
