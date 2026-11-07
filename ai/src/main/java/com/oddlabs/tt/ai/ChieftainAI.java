package com.oddlabs.tt.ai;

import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInterface;

/**
 * Base decision-making capabilities and utility computations for player chieftains.
 */
public abstract sealed class ChieftainAI permits NativeChieftainAI, VikingChieftainAI {
    public static ChieftainAI forRace(Race race) {
        return switch (race) {
            case VIKINGS -> new VikingChieftainAI();
            case NATIVES -> new NativeChieftainAI();
        };
    }

    public abstract void decide(Unit chieftain, PlayerInterface playerInterface);

    protected final int numEnemyUnits(Player owner) {
        int count = 0;
        for (Player player : owner.getWorld().getPlayers()) {
            if (owner.isEnemy(player)) {
                count += player.getUnits().size();
            }
        }
        return count;
    }
}
