package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.model.weapon.MagicFactory;
import com.oddlabs.tt.simulation.player.ChieftainAI;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Info for a playable race in the game (e.g., Natives or Vikings).
 * Defines the templates for buildings, units, and magical abilities available to the race.
 */
public final class RaceInfo {
    private final Race race;
    private final EnumMap<BuildingType, BuildingTemplate> buildings = new EnumMap<>(BuildingType.class);
    private final EnumMap<UnitType, UnitTemplate> units = new EnumMap<>(UnitType.class);
    private final List<MagicType> magics;
    private final Map<MagicType, MagicFactory> magicFactories;
    private final ChieftainAI chieftain_ai;

    public RaceInfo(Race race, BuildingTemplate quarters, BuildingTemplate armory,
            BuildingTemplate tower,
            UnitTemplate warrior_rock, UnitTemplate warrior_iron,
            UnitTemplate warrior_rubber,
            UnitTemplate peon, UnitTemplate chieftain,
            Map<MagicType, MagicFactory> magicFactories,
            ChieftainAI chieftain_ai) {
        this.race = race;
        buildings.put(BuildingType.QUARTERS, quarters);
        buildings.put(BuildingType.ARMORY, armory);
        buildings.put(BuildingType.TOWER, tower);
        units.put(UnitType.WARRIOR_ROCK, warrior_rock);
        units.put(UnitType.WARRIOR_IRON, warrior_iron);
        units.put(UnitType.WARRIOR_RUBBER, warrior_rubber);
        units.put(UnitType.PEON, peon);
        units.put(UnitType.CHIEFTAIN, chieftain);
        this.magics = switch (race) {
            case Race.NATIVES -> List.of(MagicType.POISON_FOG, MagicType.LIGHTNING_CLOUD);
            case Race.VIKINGS -> List.of(MagicType.STUN, MagicType.SONIC_BLAST);
        };
        this.magicFactories = new EnumMap<>(magicFactories);
        this.chieftain_ai = chieftain_ai;
    }

    public Race getRaceType() {
        return race;
    }

    public BuildingTemplate getBuildingTemplate(BuildingType type) {
        BuildingTemplate template = buildings.get(type);
        if (template == null) {
            throw new IllegalArgumentException("No template registered for building type: " + type);
        }
        return template;
    }

    public UnitTemplate getUnitTemplate(UnitType type) {
        UnitTemplate template = units.get(type);
        if (template == null) {
            throw new IllegalArgumentException("No template registered for unit type: " + type);
        }
        return template;
    }

    public List<MagicType> getMagics() {
        return magics;
    }

    public MagicType getMagicType(int slotIndex) {
        return magics.get(slotIndex);
    }

    public MagicFactory getMagicFactory(MagicType type) {
        MagicFactory factory = magicFactories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("No magic factory registered for magic type: " + type);
        }
        return factory;
    }

    public ChieftainAI getChieftainAI() {
        return chieftain_ai;
    }
}
