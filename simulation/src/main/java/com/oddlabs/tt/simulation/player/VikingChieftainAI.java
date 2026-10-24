package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.CountOccupantScanFilter;
import com.oddlabs.tt.simulation.pathfinder.FindOccupantFilter;

/**
 * AI logic controller for Viking chieftains, determining when to cast Stun and Sonic Blast.
 */
public final class VikingChieftainAI extends ChieftainAI {
    private static final int NUM_UNITS_FOR_STUN = 5;
    private static final int NUM_UNITS_FOR_BLAST = 7;

    @Override
    public void decide(Unit chieftain, PlayerInterface playerInterface) {
        nodeBlast(chieftain, playerInterface);
        nodeStun(chieftain, playerInterface);
    }

    private void nodeStun(Unit chieftain, PlayerInterface playerInterface) {
        if (chieftain.getMagicProgress(MagicType.STUN) < 1)
            return;

        float hit_radius = 30f;
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());
        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius, Unit.class);
        if (num_enemy_units_close >= NUM_UNITS_FOR_STUN
                || (num_enemy_units < NUM_UNITS_FOR_STUN && num_enemy_units_close > 1)
                || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1)) {
            playerInterface.doMagic(chieftain, MagicType.STUN);
        }
    }

    private void nodeBlast(Unit chieftain, PlayerInterface playerInterface) {
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
            playerInterface.doMagic(chieftain, MagicType.SONIC_BLAST);
        }
    }

    private <S extends Selectable<?>> int getNumEnemyUnitsClose(Unit chieftain, float hit_radius, Class<
            S> type) {
        if (AppConfig.LEGACY_DETERMINISTIC) {
            FindOccupantFilter<S> filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain.getPositionY(),
                    hit_radius, chieftain, type);
            chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
            int num_enemy_units_close = 0;
            for (Selectable<?> s : filter.getResult()) {
                if (s.isDead())
                    continue;

                float dx = s.getPositionX() - chieftain.getPositionX();
                float dy = s.getPositionY() - chieftain.getPositionY();
                float squared_dist = dx * dx + dy * dy;
                if (chieftain.getOwner().isEnemy(s.getOwner()) && squared_dist < hit_radius * hit_radius) {
                    num_enemy_units_close++;
                }
            }
            return num_enemy_units_close;
        }
        CountOccupantScanFilter<S> filter = new CountOccupantScanFilter<>(chieftain.getPositionX(), chieftain
                .getPositionY(),
                hit_radius, chieftain, type, s -> s.isAlive() && chieftain.getOwner().isEnemy(s.getOwner()));
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        return filter.getCount();
    }

    private int getNumFriendlyUnitsClose(Unit chieftain, float hit_radius) {
        if (AppConfig.LEGACY_DETERMINISTIC) {
            FindOccupantFilter<Selectable<?>> filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain
                    .getPositionY(),
                    hit_radius, chieftain, Selectable.genericClass());
            chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
            int num_friendly_units_close = 0;
            for (Selectable<?> s : filter.getResult()) {
                if (s.isDead())
                    continue;

                float dx = s.getPositionX() - chieftain.getPositionX();
                float dy = s.getPositionY() - chieftain.getPositionY();
                float squared_dist = dx * dx + dy * dy;
                if (!chieftain.getOwner().isEnemy(s.getOwner()) && squared_dist < hit_radius * hit_radius) {
                    num_friendly_units_close++;
                }
            }
            return num_friendly_units_close;
        }
        var filter = new CountOccupantScanFilter<>(chieftain.getPositionX(), chieftain.getPositionY(), hit_radius,
                chieftain,
                Selectable.genericClass(), s -> s.isAlive() && !chieftain.getOwner().isEnemy(s.getOwner()));
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        return filter.getCount();
    }
}
