package com.oddlabs.tt.audio;

import com.oddlabs.tt.render.Renderer;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.resource.Resources;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Manages the playback of a single audio instance associated with an {@link AudioSource}.
 */
public abstract class AudioPlayer implements Animated {
    public static final Audio SFX_AMBIENT_BEACH = Resources.findResource(new AudioFile("/sfx/ambient_beach.ogg"));
    public static final Audio SFX_AMBIENT_FOREST = Resources.findResource(new AudioFile("/sfx/ambient_forest.ogg"));
    public static final Audio SFX_AMBIENT_WIND = Resources.findResource(new AudioFile("/sfx/ambient_wind.ogg"));
    public static final Audio SFX_ARMORY = Resources.findResource(new AudioFile("/sfx/armory.ogg"));
    public static final Audio SFX_ATTACKNOTIFY_NATIVE = Resources.findResource(new AudioFile("/sfx/attacknotify_native.ogg"));
    public static final Audio SFX_ATTACKNOTIFY_VIKING = Resources.findResource(new AudioFile("/sfx/attacknotify_viking.ogg"));
    public static final @NonNull Audio @NonNull [] SFX_AXE_CUTTING_STONES = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> String.format("/sfx/axe_cutting_stone%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final @NonNull Audio @NonNull [] SFX_AXE_CUTTING_WOODS = IntStream.rangeClosed(1, 6)
            .mapToObj(i -> String.format("/sfx/axe_cutting_wood%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final Audio SFX_BUBBLING = Resources.findResource(new AudioFile("/sfx/bubbling.ogg"));
    public static final Audio SFX_BUILDINGNOTIFY_NATIVE = Resources.findResource(new AudioFile("/sfx/buildingnotify_native.ogg"));
    public static final Audio SFX_BUILDINGNOTIFY_VIKING = Resources.findResource(new AudioFile("/sfx/buildingnotify_viking.ogg"));
    public static final Audio SFX_BUILDING_CRASH = Resources.findResource(new AudioFile("/sfx/building_crash.ogg"));
    public static final Audio SFX_CHICKEN_DEATH = Resources.findResource(new AudioFile("/sfx/chicken_death.ogg"));
    public static final @NonNull Audio [] SFX_CHICKEN_IDLES = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/sfx/chicken_idle%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final Audio SFX_CHICKEN_PECK = Resources.findResource(new AudioFile("/sfx/chicken_peck.ogg"));
    public static final Audio SFX_CRACKLING_CLOUD = Resources.findResource(new AudioFile("/sfx/crackling_cloud.ogg"));
    public static final @NonNull Audio @NonNull [] SFX_DEATH_NATIVE_WARRIORS = IntStream.rangeClosed(1, 2)
            .mapToObj(i -> String.format("/sfx/death_native_warrior%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final Audio SFX_DEATH_PEON = Resources.findResource(new AudioFile("/sfx/death_peon.ogg"));
    public static final @NonNull Audio @NonNull [] SFX_DEATH_VIKING_WARRIORS = IntStream.rangeClosed(1, 2)
            .mapToObj(i -> String.format("/sfx/death_viking_warrior%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final Audio SFX_FELLING_PALMTREE = Resources.findResource(new AudioFile("/sfx/felling_palmtree.ogg"));
    public static final Audio SFX_FELLING_TREE = Resources.findResource(new AudioFile("/sfx/felling_tree.ogg"));
    public static final Audio SFX_FLASH = Resources.findResource(new AudioFile("/sfx/flash.ogg"));
    public static final Audio SFX_GAS = Resources.findResource(new AudioFile("/sfx/gas.ogg"));
    public static final @NonNull Audio @NonNull [] SFX_HITS = IntStream.rangeClosed(1, 7)
            .mapToObj(i -> String.format("/sfx/hit%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final @NonNull Audio @NonNull [] SFX_NATIVE_CHIEFTAIN_HITS = new Audio[]{
            SFX_HITS[2], SFX_HITS[3], SFX_HITS[4], SFX_HITS[5]
    };
    public static final @NonNull Audio @NonNull [] SFX_VIKING_CHIEFTAIN_HITS = new Audio[]{
            SFX_HITS[0], SFX_HITS[1], SFX_HITS[5], SFX_HITS[6]
    };
    public static final @NonNull Audio @NonNull [] SFX_IMPACT_MEATS = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> String.format("/sfx/impact_meat%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final @NonNull Audio @NonNull [] SFX_IMPACT_WOODS = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/sfx/impact_wood%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final Audio SFX_LURBLAST = Resources.findResource(new AudioFile("/sfx/lurblast.ogg"));
    public static final @NonNull Audio [] SFX_LURBLASTS = IntStream.rangeClosed(1, 3)
        .mapToObj(i -> String.format("/sfx/lur_blast%d.ogg", i)).map(AudioFile::new)
        .map(Resources::findResource).toArray(Audio[]::new);
    public static final @NonNull Audio @NonNull [] SFX_LUR_STUNS = IntStream.rangeClosed(1, 3)
            .mapToObj(i -> String.format("/sfx/lur_stun%d.ogg", i))
            .map(AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
    public static final Audio SFX_RUMBLE = Resources.findResource(new AudioFile("/sfx/rumble.ogg"));
    public static final Audio SFX_WEAPON_AXE = Resources.findResource(new AudioFile("/sfx/weapon_axe.ogg"));
    public static final Audio SFX_WEAPON_SPEAR = Resources.findResource(new AudioFile("/sfx/weapon_spear.ogg"));

    public static final AudioFile MUSIC_MENU = new AudioFile("/music/menu.ogg", true);

    public static final @NonNull Map<@NonNull Class<? extends Supply>, @NonNull Audio[]> HARVEST_SOUNDS = Map.of(
            TreeSupply.class, SFX_AXE_CUTTING_WOODS,
            RockSupply.class, SFX_AXE_CUTTING_STONES,
            IronSupply.class, SFX_AXE_CUTTING_STONES,
            RubberSupply.class, SFX_IMPACT_MEATS
    );

    public static @NonNull AudioParameters<Audio> getHarvestSound(@NonNull Class<? extends Supply> key, @NonNull Random random) {
        Audio[] sounds = HARVEST_SOUNDS.get(key);
        var audio = sounds[random.nextInt(sounds.length)];
        return new AudioParameters<>(audio, AudioPlayer.AUDIO_RANK_HARVEST,
                AudioPlayer.AUDIO_DISTANCE_HARVEST, AudioPlayer.AUDIO_GAIN_HARVEST, AudioPlayer.AUDIO_RADIUS_HARVEST);
    }

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

    public static final float AUDIO_GAIN_AMBIENT_FOREST = .01f;
    public static final float AUDIO_GAIN_AMBIENT_BEACH = .05f;
    public static final float AUDIO_GAIN_AMBIENT_WIND = .01f;
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

    public static final float AUDIO_RADIUS_AMBIENT_FOREST = 1f;
    public static final float AUDIO_RADIUS_AMBIENT_BEACH = 1f;
    public static final float AUDIO_RADIUS_AMBIENT_WIND = 1f;
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

    /** The volume threshold at which a sound is considered silent (reached at max distance). */
    static final float SILENCE_THRESHOLD = 0.032f;

    protected final @Nullable AudioSource source;
    private final @NonNull AudioParameters<?> parameters;
    protected volatile boolean playing = false;

    private float fadeout_time;
    private float end_gain;
    private float fadeout_gain;

    protected AudioPlayer(@Nullable AudioSource source, float x, float y, float z, @NonNull AudioParameters<?> params) {
        this.parameters = params;
        this.source = source;
        if (source == null || (!params.music() && !Renderer.getRenderer().getSettings().play_sfx)) {
            return;
        }
        source.setAudioPlayer(this);
        playing = true;
        
        source.setLooping(params.looping());
        source.setRelative(params.relative());

        setGain(params.gain());

        // Calculate rolloff so the sound reaches SILENCE_THRESHOLD at params.distance
        float refDist = params.radius();
        float maxDist = params.distance();
        float rolloff = (maxDist > refDist) 
                ? (refDist / SILENCE_THRESHOLD - refDist) / (maxDist - refDist) 
                : 1.0f;

        source.setRolloff(rolloff);
        source.setDistance(refDist);
        source.setMinGain(0f);
        source.setMaxGain(1f);
        source.setPitch(params.pitch());

        updateEnvironmentalEffects();
        setPosition(x, y, z);

        var state = source.getState();
        assert state == AudioSource.State.STOPPED || state == AudioSource.State.INITIAL;
    }

    protected final boolean isPlaying() {
        return playing;
    }

    public final @NonNull AudioParameters<?> getParameters() {
        return parameters;
    }

    public final void setGain(float gain) {
        if (playing && source != null) {
            var settings = Renderer.getRenderer().getSettings();
            source.setGain(gain * (parameters.music() ? settings.music_gain : settings.sound_gain));
        }
    }

    public final void setPosition(float x, float y, float z) {
        if (playing && source != null) {
            source.setPosition(x, y, z);
            updateAirAbsorption(x, y, z);
        }
    }

    private void updateEnvironmentalEffects() {
        if (source == null) return;

        // Music and notifications don't get environmental effects/reverb
        boolean useEFX = parameters.rank() != AUDIO_RANK_MUSIC && parameters.rank() != AUDIO_RANK_NOTIFICATION;

        if (Renderer.getRenderer().getAudioManager().isEFXSupported()) {
            int slot = useEFX ? Renderer.getRenderer().getAudioManager().getEFXEffectSlot() : 0;
            source.setAuxiliarySend(slot, 0);
        }
    }

    private void updateAirAbsorption(float x, float y, float z) {
        if (source == null) return;
        
        // Music doesn't get muffled by distance
        if (parameters.rank() == AUDIO_RANK_MUSIC || parameters.rank() == AUDIO_RANK_NOTIFICATION) {
            source.setDirectFilterGainHF(1.0f);
            return;
        }

        if (Renderer.getRenderer().getAudioManager().isEFXSupported()) {
            float dist = Renderer.getRenderer().getAudioManager().getListenerPosition().distance(x, y, z);

            // Simple air absorption: brighter up close, muffled far away
            // Clamp to [0.1, 1.0] to avoid total silence in HF
            float maxDist = parameters.distance() != Float.MAX_VALUE ? parameters.distance() : 1000f;
            float gainHF = Math.clamp(1.0f - (dist / maxDist), 0.1f, 1.0f);

            source.setDirectFilterGainHF(gainHF);
        }
    }

    public @NonNull AudioPlayer stop() {
        if (playing && source != null) {
            source.stop();
            playing = false;
        }

        return this;
    }

    public final @NonNull AudioPlayer registerAmbient() {
        if (source != null) {
            Renderer.getRenderer().getAudioManager().registerAmbient(source);
        }
        return this;
    }

    public final @NonNull AudioPlayer removeAmbient() {
        if (source != null) {
            Renderer.getRenderer().getAudioManager().removeAmbient(source);
        }

        return this;
    }

    public final @NonNull AudioPlayer stop(float delay, float end_gain) {
        this.end_gain = end_gain;
        fadeout_gain = end_gain;
        fadeout_time = delay;
        LocalEventQueue.getQueue().getManager().registerAnimation(this);

        return this;
    }

    @Override
    public final void animate(float t) {
        fadeout_gain -= t * (end_gain / fadeout_time);
        if (fadeout_gain <= 0) {
            stop();
            LocalEventQueue.getQueue().getManager().removeAnimation(this);
        } else {
            setGain(fadeout_gain);
        }
    }

}
