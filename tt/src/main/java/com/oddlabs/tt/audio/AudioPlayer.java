package com.oddlabs.tt.audio;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Manages the playback of a single audio instance associated with an {@link AudioSource}.
 */
public abstract class AudioPlayer implements Animated {
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
    private static final float SILENCE_THRESHOLD = 0.032f;

    protected final @Nullable AudioSource source;
    private final @NonNull AudioParameters<?> parameters;
    protected volatile boolean playing = false;

    private float fadeout_time;
    private float end_gain;
    private float fadeout_gain;

    protected AudioPlayer(@Nullable AudioSource source, @NonNull AudioParameters<?> params) {
        this.parameters = params;
        this.source = source;
        if (source == null || (!params.music && !Settings.getSettings().play_sfx)) {
            return;
        }
        source.setAudioPlayer(this);
        playing = true;
        
        source.setLooping(params.looping);
        source.setRelative(params.relative);

        setGain(params.gain);

        // Calculate rolloff so the sound reaches SILENCE_THRESHOLD at params.distance
        float refDist = params.radius;
        float maxDist = params.distance;
        float rolloff = (maxDist > refDist) 
                ? (refDist / SILENCE_THRESHOLD - refDist) / (maxDist - refDist) 
                : 1.0f;

        source.setRolloff(rolloff);
        source.setDistance(refDist);
        source.setMinGain(0f);
        source.setMaxGain(1f);
        source.setPitch(params.pitch);

        updateEnvironmentalEffects();
        setPosition(params.x, params.y, params.z);

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
            var settings = Settings.getSettings();
            source.setGain(gain * (parameters.music ? settings.music_gain : settings.sound_gain));
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
        boolean useEFX = parameters.rank != AUDIO_RANK_MUSIC && parameters.rank != AUDIO_RANK_NOTIFICATION;

        if (AudioManager.getManager().isEFXSupported()) {
            int slot = useEFX ? AudioManager.getManager().getEFXEffectSlot() : 0;
            source.setAuxiliarySend(slot, 0);
        }
    }

    private void updateAirAbsorption(float x, float y, float z) {
        if (source == null) return;
        
        // Music doesn't get muffled by distance
        if (parameters.rank == AUDIO_RANK_MUSIC || parameters.rank == AUDIO_RANK_NOTIFICATION) {
            source.setDirectFilterGainHF(1.0f);
            return;
        }

        if (AudioManager.getManager().isEFXSupported()) {
            Vector3fc listener = AudioManager.getManager().getListenerPosition();
            float dx = x - listener.x();
            float dy = y - listener.y();
            float dz = z - listener.z();
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Simple air absorption: brighter up close, muffled far away
            // Clamp to [0.1, 1.0] to avoid total silence in HF
            float maxDist = parameters.distance != Float.MAX_VALUE ? parameters.distance : 1000f;
            float gainHF = Math.clamp(1.0f - (dist / maxDist), 0.1f, 1.0f);

            source.setDirectFilterGainHF(gainHF);
        }
    }

    public void stop() {
        if (playing && source != null) {
            source.stop();
            playing = false;
        }
    }

    public final void registerAmbient() {
        if (source != null)
            AudioManager.getManager().registerAmbient(source);
    }

    public final void removeAmbient() {
        if (source != null)
            AudioManager.getManager().removeAmbient(source);
    }

    public final void stop(float delay, float end_gain) {
        this.end_gain = end_gain;
        fadeout_gain = end_gain;
        fadeout_time = delay;
        LocalEventQueue.getQueue().getManager().registerAnimation(this);
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
