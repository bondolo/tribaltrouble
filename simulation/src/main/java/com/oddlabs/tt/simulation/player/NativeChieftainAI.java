package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.CountOccupantScanFilter;
import com.oddlabs.tt.simulation.pathfinder.FindOccupantFilter;

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
        if (chieftain.getMagicProgress(MagicType.LIGHTNING_CLOUD) < 1)
            return;

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
        if (chieftain.getMagicProgress(MagicType.POISON_FOG) < 1)
            return;

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

    private int getNumEnemyUnitsClose(Unit chieftain, float hit_radius) {
        if (AppConfig.LEGACY_DETERMINISTIC) {
            FindOccupantFilter<Unit> filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain
                    .getPositionY(),
                    hit_radius, chieftain, Unit.class);
            chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
            int num_enemy_units_close = 0;
            for (Unit unit : filter.getResult()) {
                if (unit.isDead())
                    continue;

                float dx = unit.getPositionX() - chieftain.getPositionX();
                float dy = unit.getPositionY() - chieftain.getPositionY();
                float squared_dist = dx * dx + dy * dy;
                if (chieftain.getOwner().isEnemy(unit.getOwner()) && squared_dist < hit_radius * hit_radius) {
                    num_enemy_units_close++;
                }
            }
            return num_enemy_units_close;
        }
        var filter = new CountOccupantScanFilter<>(chieftain.getPositionX(), chieftain.getPositionY(), hit_radius,
                chieftain,
                Unit.class, unit -> unit.isAlive() && chieftain.getOwner().isEnemy(unit.getOwner()));
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
