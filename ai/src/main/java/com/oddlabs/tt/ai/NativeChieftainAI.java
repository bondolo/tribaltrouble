package com.oddlabs.tt.ai;

import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.PlayerInterface;

/**
 * AI logic controller for Native chieftains, determining when to cast Poison Fog and Lightning Cloud.
 */
public final class NativeChieftainAI extends ChieftainAI {
    private static final int NUM_UNITS_FOR_LIGHTNING = 2;
    private static final int NUM_UNITS_FOR_POISON = 5;

    @Override
    public void decide(Unit chieftain, PlayerInterface playerInterface) {
        nodeLightningCloud(chieftain, playerInterface);
        nodePoisonFog(chieftain, playerInterface);
    }

    private void nodeLightningCloud(Unit chieftain, PlayerInterface playerInterface) {
        if (chieftain.getMagicProgress(MagicType.LIGHTNING_CLOUD) < 1) {
            return;
        }

        float hit_radius = 30f;
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());
        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius);
        if (num_enemy_units_close >= NUM_UNITS_FOR_LIGHTNING
                || (num_enemy_units < NUM_UNITS_FOR_LIGHTNING && num_enemy_units_close > 1)
                || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1)) {
            playerInterface.doMagic(chieftain, MagicType.LIGHTNING_CLOUD);
        }
    }

    private void nodePoisonFog(Unit chieftain, PlayerInterface playerInterface) {
        if (chieftain.getMagicProgress(MagicType.POISON_FOG) < 1) {
            return;
        }

        float hit_radius = chieftain.getOwner().getRaceInfo().getMagicFactory(MagicType.POISON_FOG)
                .getHitRadius();
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());
        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius);
        int num_friendly_units_close = getNumFriendlyUnitsClose(chieftain, hit_radius);
        if (2 * num_friendly_units_close < num_enemy_units_close
                && (num_enemy_units_close >= NUM_UNITS_FOR_POISON
                        || (num_enemy_units < NUM_UNITS_FOR_POISON && num_enemy_units_close > 1)
                        || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1))) {
            playerInterface.doMagic(chieftain, MagicType.POISON_FOG);
        }
    }
}
