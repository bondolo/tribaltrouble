package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.gui.RaceIcons;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.player.ChieftainAI;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

/**
 * Represents a playable race in the game (e.g., Natives or Vikings).
 * Defines the templates for buildings, units, and magical abilities available to the race.
 */
public final class Race {
    public static final int BUILDING_QUARTERS = 0;
    public static final int BUILDING_ARMORY = 1;
    public static final int BUILDING_TOWER = 2;

    public static final int NUM_BUILDINGS = 3;

    public static final int UNIT_WARRIOR_ROCK = 0;
    public static final int UNIT_WARRIOR_IRON = 1;
    public static final int UNIT_WARRIOR_RUBBER = 2;
    public static final int UNIT_PEON = 3;
    public static final int UNIT_CHIEFTAIN = 4;

    private final int raceType;
    private final @NonNull BuildingTemplate[] buildings = new BuildingTemplate[NUM_BUILDINGS];
    private final @NonNull UnitTemplate[] units = new UnitTemplate[5];
    private final @NonNull RaceIcons icons;
    private final @NonNull AudioParameters attack_notification;
    private final @NonNull AudioParameters building_notification;
    private final @NonNull MagicFactory @NonNull [] magic_factory;
    private final @NonNull ChieftainAI chieftain_ai;
    private final @NonNull AudioParameters music;

    public Race(int raceType, @NonNull BuildingTemplate quarters, @NonNull BuildingTemplate armory,
            @NonNull BuildingTemplate tower,
            @NonNull UnitTemplate warrior_rock, @NonNull UnitTemplate warrior_iron,
            @NonNull UnitTemplate warrior_rubber,
            @NonNull UnitTemplate peon, @NonNull UnitTemplate chieftain,
            @NonNull RaceIcons icons,
            @NonNull AudioFile attack_notification, @NonNull AudioFile building_notification,
            @NonNull MagicFactory @NonNull [] magic_factory,
            @NonNull ChieftainAI chieftain_ai,
            @NonNull AudioParameters music) {
        this.raceType = raceType;
        buildings[BUILDING_QUARTERS] = quarters;
        buildings[BUILDING_ARMORY] = armory;
        buildings[BUILDING_TOWER] = tower;
        for (int i = 0; i < buildings.length; i++) {
            assert buildings[i].getTemplateID() == i;
        }
        units[UNIT_WARRIOR_ROCK] = warrior_rock;
        units[UNIT_WARRIOR_IRON] = warrior_iron;
        units[UNIT_WARRIOR_RUBBER] = warrior_rubber;
        units[UNIT_PEON] = peon;
        units[UNIT_CHIEFTAIN] = chieftain;
        this.icons = icons;
        this.attack_notification = new AudioParameters(attack_notification, AudioAssets.AUDIO_RANK_NOTIFICATION,
                AudioAssets.AUDIO_DISTANCE_NOTIFICATION, AudioAssets.AUDIO_GAIN_NOTIFICATION,
                AudioAssets.AUDIO_RADIUS_NOTIFICATION,
                1f, false, true);
        this.building_notification = new AudioParameters(building_notification, AudioAssets.AUDIO_RANK_NOTIFICATION,
                AudioAssets.AUDIO_DISTANCE_NOTIFICATION, AudioAssets.AUDIO_GAIN_NOTIFICATION,
                AudioAssets.AUDIO_RADIUS_NOTIFICATION,
                1f, false, true);
        this.magic_factory = magic_factory;
        this.chieftain_ai = chieftain_ai;
        this.music = music;
    }

    public int getRaceType() {
        return raceType;
    }

    public @NonNull BuildingTemplate getBuildingTemplate(int index) {
        return buildings[index];
    }

    public @NonNull UnitTemplate getUnitTemplate(int index) {
        return units[index];
    }

    public @NonNull RaceIcons getIcons() {
        return icons;
    }

    public @NonNull AudioParameters getAttackNotificationAudio() {
        return attack_notification;
    }

    public @NonNull AudioParameters getBuildingNotificationAudio() {
        return building_notification;
    }

    public @NonNull MagicFactory getMagicFactory(int i) {
        return magic_factory[i];
    }

    public @NonNull ChieftainAI getChieftainAI() {
        return chieftain_ai;
    }

    public @NonNull AudioParameters getMusic() {
        return music;
    }
}
