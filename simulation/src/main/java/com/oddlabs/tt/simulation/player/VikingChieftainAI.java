package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.CountOccupantScanFilter;

/**
 * AI logic controller for Viking chieftains, determining when to cast Stun and Sonic Blast.
 */
public final class VikingChieftainAI extends ChieftainAI {
    private static final int NUM_UNITS_FOR_STUN = 5;
    private static final int NUM_UNITS_FOR_BLAST = 7;

    @Override
    public void decide(Unit chieftain) {
        nodeBlast(chieftain);
        nodeStun(chieftain);
    }

    private void nodeStun(Unit chieftain) {
        if (chieftain.getMagicProgress(MagicType.STUN) < 1)
            return;

        float hit_radius = 30f;
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());
        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius, Unit.class);
        if (num_enemy_units_close >= NUM_UNITS_FOR_STUN
                || (num_enemy_units < NUM_UNITS_FOR_STUN && num_enemy_units_close > 1)
                || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1)) {
            chieftain.doMagic(MagicType.STUN, false);
        }
    }

    private void nodeBlast(Unit chieftain) {
        if (chieftain.getMagicProgress(MagicType.SONIC_BLAST) < 1)
            return;

        float hit_radius = chieftain.getOwner().getRaceInfo().getMagicFactory(MagicType.SONIC_BLAST).getHitRadius();
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());

        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius, Selectable.genericClass());
        int num_friendly_units_close = getNumFriendlyUnitsClose(chieftain, hit_radius);
        if (2 * num_friendly_units_close < num_enemy_units_close
                && (num_enemy_units_close >= NUM_UNITS_FOR_BLAST
                        || (num_enemy_units < NUM_UNITS_FOR_BLAST && num_enemy_units_close > 1)
                        || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1))) {
            chieftain.doMagic(MagicType.SONIC_BLAST, false);
        }
    }

    private <S extends Selectable<?>> int getNumEnemyUnitsClose(Unit chieftain, float hit_radius, Class<
            S> type) {
        CountOccupantScanFilter<S> filter = new CountOccupantScanFilter<>(chieftain.getPositionX(), chieftain.getPositionY(),
                hit_radius, chieftain, type, s -> s.isAlive() && chieftain.getOwner().isEnemy(s.getOwner()));
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        return filter.getCount();
    }

    private int getNumFriendlyUnitsClose(Unit chieftain, float hit_radius) {
        var filter = new CountOccupantScanFilter<>(chieftain.getPositionX(), chieftain.getPositionY(), hit_radius, chieftain,
                Selectable.genericClass(), s -> s.isAlive() && !chieftain.getOwner().isEnemy(s.getOwner()));
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        return filter.getCount();
    }
}
