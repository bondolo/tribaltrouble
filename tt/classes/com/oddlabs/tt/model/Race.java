package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.gui.RaceIcons;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.player.ChieftainAI;
import com.oddlabs.tt.render.SpriteKey;
import java.util.EnumMap;
import java.util.Map;

public final strictfp class Race {
    public enum BuildingType {
        QUARTERS,
        ARMORY,
        TOWER;

        private static final BuildingType[] VALUES = values();
        public static final int LENGTH = VALUES.length;
    };

    public enum UnitType {
        WARRIOR_ROCK,
        WARRIOR_IRON,
        WARRIOR_RUBBER,
        PEON,
        CHIEFTAIN
    }

	private final EnumMap<BuildingType,BuildingTemplate> buildings = new EnumMap<>(BuildingType.class);
	private final EnumMap<UnitType,UnitTemplate> units = new EnumMap<>(UnitType.class);
	private final SpriteKey rally_point;
	private final RaceIcons icons;
	private final Audio attack_notification;
	private final Audio building_notification;
	private final MagicFactory[] magic_factory;
	private final ChieftainAI chieftain_ai;
	private final String music_path;

	public Race(BuildingTemplate quarters,
			BuildingTemplate armory,
			BuildingTemplate tower,
			UnitTemplate warrior_rock,
			UnitTemplate warrior_iron,
			UnitTemplate warrior_rubber,
			UnitTemplate peon,
			UnitTemplate chieftain,
			SpriteKey rally_point,
			RaceIcons icons,
			Audio attack_notification,
			Audio building_notification,
			MagicFactory[] magic_factory,
			ChieftainAI chieftain_ai,
			String music_path) {
		buildings.put(BuildingType.QUARTERS, quarters);
		buildings.put(BuildingType.ARMORY, armory);
		buildings.put(BuildingType.TOWER, tower);
		for (Map.Entry<BuildingType,BuildingTemplate> entry : buildings.entrySet()) {
            assert entry.getValue().getTemplateID() == entry.getKey();
        }
        units.put(UnitType.WARRIOR_ROCK, warrior_rock);
        units.put(UnitType.WARRIOR_IRON, warrior_iron);
        units.put(UnitType.WARRIOR_RUBBER, warrior_rubber);
        units.put(UnitType.PEON, peon);
        units.put(UnitType.CHIEFTAIN, chieftain);
		this.rally_point = rally_point;
		this.icons = icons;
		this.attack_notification = attack_notification;
		this.building_notification = building_notification;
		this.magic_factory = magic_factory;
		this.chieftain_ai = chieftain_ai;
		this.music_path = music_path;
	}

	public BuildingTemplate getBuildingTemplate(Race.BuildingType index) {
		return buildings.get(index);
	}

	public UnitTemplate getUnitTemplate(UnitType index) {
		return units.get(index);
	}

	public SpriteKey getRallyPoint() {
		return rally_point;
	}

	public RaceIcons getIcons() {
		return icons;
	}

	public Audio getAttackNotificationAudio() {
		return attack_notification;
	}

	public Audio getBuildingNotificationAudio() {
		return building_notification;
	}

	public MagicFactory getMagicFactory(int i) {
		return magic_factory[i];
	}

	public ChieftainAI getChieftainAI() {
		return chieftain_ai;
	}

	public String getMusicPath() {
		return music_path;
	}
}
