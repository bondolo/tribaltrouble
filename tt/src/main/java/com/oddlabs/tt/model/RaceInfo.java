package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.player.ChieftainAI;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

/**
 * Info for a playable race in the game (e.g., Natives or Vikings).
 * Defines the templates for buildings, units, and magical abilities available to the race.
 */
public final class RaceInfo {
    private final @NonNull Race race;
    private final @NonNull BuildingTemplate[] buildings = new BuildingTemplate[BuildingType.values().length];
    private final @NonNull UnitTemplate[] units = new UnitTemplate[5];
    private final @NonNull AudioParameters attack_notification;
    private final @NonNull AudioParameters building_notification;
    private final @NonNull MagicFactory @NonNull [] magic_factory;
    private final @NonNull ChieftainAI chieftain_ai;
    private final @NonNull AudioParameters music;

    public RaceInfo(@NonNull Race race, @NonNull BuildingTemplate quarters, @NonNull BuildingTemplate armory,
            @NonNull BuildingTemplate tower,
            @NonNull UnitTemplate warrior_rock, @NonNull UnitTemplate warrior_iron,
            @NonNull UnitTemplate warrior_rubber,
            @NonNull UnitTemplate peon, @NonNull UnitTemplate chieftain,
            @NonNull AudioFile attack_notification, @NonNull AudioFile building_notification,
            @NonNull MagicFactory @NonNull [] magic_factory,
            @NonNull ChieftainAI chieftain_ai,
            @NonNull AudioParameters music) {
        this.race = race;
        buildings[BuildingType.QUARTERS.ordinal()] = quarters;
        buildings[BuildingType.ARMORY.ordinal()] = armory;
        buildings[BuildingType.TOWER.ordinal()] = tower;
        units[UnitType.WARRIOR_ROCK.ordinal()] = warrior_rock;
        units[UnitType.WARRIOR_IRON.ordinal()] = warrior_iron;
        units[UnitType.WARRIOR_RUBBER.ordinal()] = warrior_rubber;
        units[UnitType.PEON.ordinal()] = peon;
        units[UnitType.CHIEFTAIN.ordinal()] = chieftain;
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

    public @NonNull Race getRaceType() {
        return race;
    }

    public @NonNull BuildingTemplate getBuildingTemplate(@NonNull BuildingType type) {
        return buildings[type.ordinal()];
    }

    public @NonNull UnitTemplate getUnitTemplate(@NonNull UnitType type) {
        return units[type.ordinal()];
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
