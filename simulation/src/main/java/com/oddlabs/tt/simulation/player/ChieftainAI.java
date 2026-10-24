package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Unit;


/**
 * Chieftain AI, implementing base decision-making capabilities and utility computations for player chieftains.
 */
public abstract sealed class ChieftainAI permits NativeChieftainAI, VikingChieftainAI {
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
