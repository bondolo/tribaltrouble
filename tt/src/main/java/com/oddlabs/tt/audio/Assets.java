package com.oddlabs.tt.audio;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Supply;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/** Constants for all the game audio assets and parameters */
public class Assets {

    // Sound effects
    public static final AudioFile SFX_AMBIENT_BEACH = new AudioFile("/sfx/ambient_beach.ogg");
    public static final AudioFile SFX_AMBIENT_FOREST = new AudioFile("/sfx/ambient_forest.ogg");
    public static final AudioFile SFX_AMBIENT_WIND = new AudioFile("/sfx/ambient_wind.ogg");
    public static final AudioFile SFX_ARMORY = new AudioFile("/sfx/armory.ogg");
    public static final AudioFile SFX_ATTACKNOTIFY_NATIVE = new AudioFile("/sfx/attacknotify_native.ogg");
    public static final AudioFile SFX_ATTACKNOTIFY_VIKING = new AudioFile("/sfx/attacknotify_viking.ogg");
    public static final @NonNull AudioFile @NonNull [] SFX_AXE_CUTTING_STONES = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> String.format("/sfx/axe_cutting_stone%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final @NonNull AudioFile @NonNull [] SFX_AXE_CUTTING_WOODS = IntStream.rangeClosed(1, 6)
            .mapToObj(i -> String.format("/sfx/axe_cutting_wood%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_BUBBLING = new AudioFile("/sfx/bubbling.ogg");
    public static final AudioFile SFX_BUILDINGNOTIFY_NATIVE = new AudioFile("/sfx/buildingnotify_native.ogg");
    public static final AudioFile SFX_BUILDINGNOTIFY_VIKING = new AudioFile("/sfx/buildingnotify_viking.ogg");
    public static final AudioFile SFX_BUILDING_CRASH = new AudioFile("/sfx/building_crash.ogg");
    public static final AudioFile SFX_CHICKEN_DEATH = new AudioFile("/sfx/chicken_death.ogg");
    public static final @NonNull AudioFile[] SFX_CHICKEN_IDLES = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/sfx/chicken_idle%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_CHICKEN_PECK = new AudioFile("/sfx/chicken_peck.ogg");
    public static final AudioFile SFX_CRACKLING_CLOUD = new AudioFile("/sfx/crackling_cloud.ogg");
    public static final @NonNull AudioFile @NonNull [] SFX_DEATH_NATIVE_WARRIORS = IntStream.rangeClosed(1, 2)
            .mapToObj(i -> String.format("/sfx/death_native_warrior%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_DEATH_PEON = new AudioFile("/sfx/death_peon.ogg");
    public static final @NonNull AudioFile @NonNull [] SFX_DEATH_VIKING_WARRIORS = IntStream.rangeClosed(1, 2)
            .mapToObj(i -> String.format("/sfx/death_viking_warrior%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_FELLING_PALMTREE = new AudioFile("/sfx/felling_palmtree.ogg");
    public static final AudioFile SFX_FELLING_TREE = new AudioFile("/sfx/felling_tree.ogg");
    public static final AudioFile SFX_FLASH = new AudioFile("/sfx/flash.ogg");
    public static final AudioFile SFX_GAS = new AudioFile("/sfx/gas.ogg");
    public static final AudioFile SFX_RUMBLE = new AudioFile("/sfx/rumble.ogg");
    public static final AudioFile SFX_WEAPON_AXE = new AudioFile("/sfx/weapon_axe.ogg");
    public static final AudioFile SFX_WEAPON_SPEAR = new AudioFile("/sfx/weapon_spear.ogg");

    public static final @NonNull AudioFile @NonNull [] SFX_HITS = IntStream.rangeClosed(1, 7)
            .mapToObj(i -> String.format("/sfx/hit%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final @NonNull AudioFile @NonNull [] SFX_VIKING_CHIEFTAIN_HITS = new AudioFile[]{
            SFX_HITS[0], SFX_HITS[1], SFX_HITS[5], SFX_HITS[6]
    };
    public static final @NonNull AudioFile @NonNull [] SFX_NATIVE_CHIEFTAIN_HITS = new AudioFile[]{
            SFX_HITS[2], SFX_HITS[3], SFX_HITS[4], SFX_HITS[5]
    };
    public static final @NonNull AudioFile @NonNull [] SFX_IMPACT_MEATS = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> String.format("/sfx/impact_meat%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);

    public static final @NonNull AudioFile @NonNull [] SFX_IMPACT_WOODS = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/sfx/impact_wood%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_LURBLAST = new AudioFile("/sfx/lurblast.ogg");
    public static final @NonNull AudioFile[] SFX_LURBLASTS = IntStream.rangeClosed(1, 3)
            .mapToObj(i -> String.format("/sfx/lur_blast%d.ogg", i)).map(AudioFile::new).toArray(AudioFile[]::new);
    public static final @NonNull AudioFile @NonNull [] SFX_LUR_STUNS = IntStream.rangeClosed(1, 3)
            .mapToObj(i -> String.format("/sfx/lur_stun%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);

    public static final @NonNull Map<@NonNull Class<? extends Supply>, @NonNull AudioFile[]> SFX_HARVEST_SOUNDS = Map
            .of(
                    TreeSupply.class, SFX_AXE_CUTTING_WOODS,
                    RockSupply.class, SFX_AXE_CUTTING_STONES,
                    IronSupply.class, SFX_AXE_CUTTING_STONES,
                    RubberSupply.class, SFX_IMPACT_MEATS
            );

    // Music
    public static final AudioFile MUSIC_MENU = new AudioFile("/music/menu.ogg");
    public static final AudioFile MUSIC_NATIVE = new AudioFile("/music/native.ogg");
    public static final AudioFile MUSIC_VIKING = new AudioFile("/music/viking.ogg");

    // Sound priorit rankings
    public static final int AUDIO_RANK_AMBIENT = 75;
    public static final int AUDIO_RANK_MUSIC = 50;
    public static final int AUDIO_RANK_NOTIFICATION = 40;
    public static final int AUDIO_RANK_BUILDING_COLLAPSE = 20;
    public static final int AUDIO_RANK_DEATH = 10;
    public static final int AUDIO_RANK_MAGIC = 8;
    public static final int AUDIO_RANK_WEAPON_HIT = 7;
    public static final int AUDIO_RANK_WEAPON_ATTACK = 6;
    public static final int AUDIO_RANK_TREE_FALL = 5;
    public static final int AUDIO_RANK_GAS = 4;
    public static final int AUDIO_RANK_ARMORY = 3;
    public static final int AUDIO_RANK_HARVEST = 2;
    public static final int AUDIO_RANK_CHICKEN = 1;
    public static final int AUDIO_RANK_NOT_INITIALIZED = 0;

    // Sound distance parameters
    public static final float AUDIO_DISTANCE_MUSIC = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_AMBIENT = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_NOTIFICATION = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_BUILDING_COLLAPSE = 150f;
    public static final float AUDIO_DISTANCE_DEATH = 100f;
    public static final float AUDIO_DISTANCE_MAGIC = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_WEAPON_HIT = 75f;
    public static final float AUDIO_DISTANCE_WEAPON_ATTACK = 75f;
    public static final float AUDIO_DISTANCE_TREE_FALL = 80f;
    public static final float AUDIO_DISTANCE_ARMORY = 120f;
    public static final float AUDIO_DISTANCE_HARVEST = 40f;
    public static final float AUDIO_DISTANCE_CHICKEN = 25f;

    // Sound gain parameters
    public static final float AUDIO_GAIN_AMBIENT_FOREST = .01f;
    public static final float AUDIO_GAIN_AMBIENT_BEACH = .05f;
    public static final float AUDIO_GAIN_AMBIENT_WIND = .01f;
    public static final float AUDIO_GAIN_NOTIFICATION = 0.25f;
    public static final float AUDIO_GAIN_BUILDING_COLLAPSE = 1f;
    public static final float AUDIO_GAIN_WEAPON_HIT = .5f;
    public static final float AUDIO_GAIN_WEAPON_ATTACK = 1f;
    public static final float AUDIO_GAIN_HARVEST = 1f;
    public static final float AUDIO_GAIN_CHICKEN_IDLE = .25f;
    public static final float AUDIO_GAIN_CHICKEN_PECK = .25f;
    public static final float AUDIO_GAIN_CHICKEN_DEATH = .25f;
    public static final float AUDIO_GAIN_DEATH = 1f;
    public static final float AUDIO_GAIN_TREE_FALL = 1f;
    public static final float AUDIO_GAIN_LIGHTNING = 1f;
    public static final float AUDIO_GAIN_CLOUD = .4f;
    public static final float AUDIO_GAIN_BUBBLING = 1f;
    public static final float AUDIO_GAIN_GAS = .25f;
    public static final float AUDIO_GAIN_STUN_LUR = 1f;
    public static final float AUDIO_GAIN_BLAST_LUR = 1f;
    public static final float AUDIO_GAIN_BLAST_RUMBLE = 1f;
    public static final float AUDIO_GAIN_BLAST_BLAST = 1f;
    public static final float AUDIO_GAIN_ARMORY = 1f;

    // Sound radius parameters
    public static final float AUDIO_RADIUS_AMBIENT_FOREST = 1f;
    public static final float AUDIO_RADIUS_AMBIENT_BEACH = 1f;
    public static final float AUDIO_RADIUS_AMBIENT_WIND = 1f;
    public static final float AUDIO_RADIUS_NOTIFICATION = 1f;
    public static final float AUDIO_RADIUS_BUILDING_COLLAPSE = 5f;
    public static final float AUDIO_RADIUS_WEAPON_HIT = 1f;
    public static final float AUDIO_RADIUS_WEAPON_ATTACK = 1f;
    public static final float AUDIO_RADIUS_HARVEST = .5f;
    public static final float AUDIO_RADIUS_CHICKEN_IDLE = .1f;
    public static final float AUDIO_RADIUS_CHICKEN_PECK = .1f;
    public static final float AUDIO_RADIUS_CHICKEN_DEATH = .1f;
    public static final float AUDIO_RADIUS_DEATH = 1f;
    public static final float AUDIO_RADIUS_TREE_FALL = 2f;
    public static final float AUDIO_RADIUS_LIGHTNING = 5f;
    public static final float AUDIO_RADIUS_CLOUD = 5f;
    public static final float AUDIO_RADIUS_BUBBLING = 1f;
    public static final float AUDIO_RADIUS_GAS = .5f;
    public static final float AUDIO_RADIUS_STUN_LUR = 1f;
    public static final float AUDIO_RADIUS_BLAST_LUR = 1f;
    public static final float AUDIO_RADIUS_BLAST_RUMBLE = 1f;
    public static final float AUDIO_RADIUS_BLAST_BLAST = 1f;
    public static final float AUDIO_RADIUS_ARMORY = 5f;

    public static @NonNull AudioParameters getHarvestSound(@NonNull Class<? extends Supply> key,
            @NonNull Random random) {
        AudioFile[] sounds = SFX_HARVEST_SOUNDS.get(key);
        var audioFile = sounds[random.nextInt(sounds.length)];
        return new AudioParameters(audioFile, AUDIO_RANK_HARVEST,
                AUDIO_DISTANCE_HARVEST, AUDIO_GAIN_HARVEST, AUDIO_RADIUS_HARVEST);
    }

    private Assets() {
        // No instances
    }
}
