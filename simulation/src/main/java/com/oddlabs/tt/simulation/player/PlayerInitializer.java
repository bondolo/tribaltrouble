package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Initializes deterministic starting bases, armies, and speeds for players at tick 0.
 */
public final class PlayerInitializer {

    private static final float RADIAL_SEARCH_RADIUS = 30f;

    private PlayerInitializer() {
    }

    /**
     * Initializes a player's starting entities (base, army, or tutorial units) according to slot configuration.
     */
    public static void initPlayer(
            Player player,
            PlayerSlot slot,
            UnitInfo unitInfo,
            List<SpawnLocation> startingLocations,
            int initialGamespeed,
            @Nullable String chieftainName) {
        if (slot.getType() == PlayerSlot.AI) {
            switch (slot.getAIDifficulty()) {
                case PlayerSlot.AI_TOWER_TUTORIAL -> {
                    // Tower tutorial creates no base or units for this AI slot.
                }
                case PlayerSlot.AI_CHIEFTAIN_TUTORIAL -> {
                    new Unit(player, 100, 100, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                    new Unit(player, 200, 100, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                    new Unit(player, 40, 200, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
                }
                default -> initializePlayerBase(player, unitInfo);
            }
        } else {
            initializePlayerArmy(player, startingLocations, unitInfo, initialGamespeed, chieftainName);
        }
    }

    /**
     * Initializes a player's starting base (buildings, chieftain, and units) using radial placement.
     */
    public static void initializePlayerBase(Player owner, UnitInfo unit_info) {
        int grid_start_x = UnitGrid.toGridCoordinate(owner.getStartX());
        int grid_start_y = UnitGrid.toGridCoordinate(owner.getStartY());
        if (unit_info.hasQuarters()) {
            owner.buildBuilding(BuildingType.QUARTERS, grid_start_x, grid_start_y);
        }
        if (unit_info.hasArmory()) {
            owner.buildBuilding(BuildingType.ARMORY, grid_start_x, grid_start_y);
        }
        for (int i = 0; i < unit_info.numTowers(); i++) {
            int center = owner.getWorld().getHeightMap().getGridUnitsPerWorld() / 2;
            int dx = center - grid_start_x;
            int dy = center - grid_start_y;
            float inv_dist = 1f / (float) Math.sqrt(dx * dx + dy * dy);
            int tx = (int) (grid_start_x + 10f * dx * inv_dist);
            int ty = (int) (grid_start_y + 10f * dy * inv_dist);
            owner.buildBuilding(BuildingType.TOWER, tx, ty);
        }
        Random random = new Random(42);
        if (unit_info.hasChieftain()) {
            Target t = getRadialTarget(owner, random);
            Unit chieftain = new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo()
                    .getUnitTemplate(UnitType.CHIEFTAIN));
            owner.setActiveChieftain(chieftain);
        }
        for (int i = 0; i < unit_info.numPeons(); i++) {
            Target t = getRadialTarget(owner, random);
            new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo().getUnitTemplate(
                    UnitType.PEON));
        }
        for (int i = 0; i < unit_info.numRockWarriors(); i++) {
            Target t = getRadialTarget(owner, random);
            new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_ROCK));
        }
        for (int i = 0; i < unit_info.numIronWarriors(); i++) {
            Target t = getRadialTarget(owner, random);
            new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_IRON));
        }
        for (int i = 0; i < unit_info.numRubberWarriors(); i++) {
            Target t = getRadialTarget(owner, random);
            new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo().getUnitTemplate(
                    UnitType.WARRIOR_RUBBER));
        }
    }

    /**
     * Initializes a player's starting army using explicit spawn locations.
     */
    public static void initializePlayerArmy(
            Player player,
            List<SpawnLocation> startingLocations,
            UnitInfo unitInfo,
            int initialGamespeed,
            @Nullable String chieftainName) {
        player.setPreferredGamespeed(initialGamespeed);
        var raceInfo = player.getRaceInfo();

        if (AppConfig.LEGACY_DETERMINISTIC) {
            Random random = new Random(42);
            if (unitInfo.hasChieftain()) {
                Target t = getLegacyTarget(player, random);
                Unit chieftain = chieftainName != null
                        ? new Unit(player, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.CHIEFTAIN),
                                chieftainName, false)
                        : new Unit(player, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.CHIEFTAIN));
                raceInfo.getMagics().forEach(chieftain::maxMagicEnergy);
                player.setActiveChieftain(chieftain);
            }
            for (int j = 0; j < unitInfo.numPeons(); j++) {
                Target t = getLegacyTarget(player, random);
                new Unit(player, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.PEON));
            }
            for (int j = 0; j < unitInfo.numRockWarriors(); j++) {
                Target t = getLegacyTarget(player, random);
                new Unit(player, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_ROCK));
            }
            for (int j = 0; j < unitInfo.numIronWarriors(); j++) {
                Target t = getLegacyTarget(player, random);
                new Unit(player, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_IRON));
            }
            for (int j = 0; j < unitInfo.numRubberWarriors(); j++) {
                Target t = getLegacyTarget(player, random);
                new Unit(player, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_RUBBER));
            }
            return;
        }

        int idx = 0;
        for (int j = 0; j < unitInfo.numPeons(); j++, idx++) {
            SpawnLocation loc = startingLocations.get(idx);
            new Unit(player, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.PEON));
        }
        for (int j = 0; j < unitInfo.numRockWarriors(); j++, idx++) {
            SpawnLocation loc = startingLocations.get(idx);
            new Unit(player, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_ROCK));
        }
        for (int j = 0; j < unitInfo.numIronWarriors(); j++, idx++) {
            SpawnLocation loc = startingLocations.get(idx);
            new Unit(player, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_IRON));
        }
        for (int j = 0; j < unitInfo.numRubberWarriors(); j++, idx++) {
            SpawnLocation loc = startingLocations.get(idx);
            new Unit(player, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_RUBBER));
        }
        if (unitInfo.hasChieftain()) {
            SpawnLocation loc = startingLocations.get(idx);
            Unit chieftain = chieftainName != null
                    ? new Unit(player, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.CHIEFTAIN),
                            chieftainName, false)
                    : new Unit(player, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.CHIEFTAIN));
            raceInfo.getMagics().forEach(chieftain::maxMagicEnergy);
            player.setActiveChieftain(chieftain);
        }
    }

    public static Target getRadialTarget(Player owner, RandomGenerator random) {
        float target_x = owner.getStartX() + random.nextFloat(-RADIAL_SEARCH_RADIUS, RADIAL_SEARCH_RADIUS);
        float target_y = owner.getStartY() + random.nextFloat(-RADIAL_SEARCH_RADIUS, RADIAL_SEARCH_RADIUS);
        Target target = owner.getWorld().getUnitGrid().findGridTarget(UnitGrid.toGridCoordinate(target_x),
                UnitGrid.toGridCoordinate(target_y), false);
        return target != null
                ? target
                : new LandscapeTarget(UnitGrid.toGridCoordinate(target_x), UnitGrid.toGridCoordinate(target_y));
    }

    private static Target getLegacyTarget(Player owner, Random random) {
        float radius = 30f;
        float target_x = owner.getStartX() + (random.nextFloat() * 2f - 1f) * radius;
        float target_y = owner.getStartY() + (random.nextFloat() * 2f - 1f) * radius;
        Target[] targets = owner.getWorld().getUnitGrid().findGridTargets(
                UnitGrid.toGridCoordinate(target_x),
                UnitGrid.toGridCoordinate(target_y),
                1,
                false
        );
        return targets[0];
    }
}
