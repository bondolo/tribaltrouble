package com.oddlabs.tt.audio;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Manages the playback of a single audio instance associated with an {@link AudioSource}.
 */
public abstract class AudioPlayer implements Animated {

    /** The volume threshold at which a sound is considered silent (reached at max distance). */
    static final float SILENCE_THRESHOLD = 0.032f;

    protected final @Nullable AudioSource source;
    protected final @NonNull AudioParameters parameters;
    protected volatile boolean playing = false;

    private float fadeout_time;
    private float end_gain;
    private float fadeout_gain;

    protected AudioPlayer(@Nullable AudioSource source, float x, float y, float z, @NonNull AudioParameters params) {
        this.parameters = params;
        this.source = source;
        AudioManager audioManager = Renderer.getRenderer().getAudioManager();
        if (source == null || (!params.audio().isStreaming() && !audioManager.isSfxEnabled())) {
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

    public final @NonNull AudioParameters getParameters() {
        return parameters;
    }

    /** {@return The audio associated with this player} */
    protected @NonNull Audio getAudio() {
        return parameters.audio().get();
    }

    /** {@return The number of buffers allocated for this player} */
    protected int getBufferCount() {
        return 0;
    }

    public final void setGain(float gain) {
        if (playing && source != null) {
            AudioManager audioManager = Renderer.getRenderer().getAudioManager();
            source.setGain(gain * (parameters.audio().isStreaming() ? audioManager.getMusicGain() : audioManager
                    .getSfxGain()));
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
        boolean useEFX = parameters.rank() != Assets.AUDIO_RANK_MUSIC && parameters.rank()
                != Assets.AUDIO_RANK_NOTIFICATION;

        if (Renderer.getRenderer().getAudioManager().isEFXSupported()) {
            int slot = useEFX ? Renderer.getRenderer().getAudioManager().getEFXEffectSlot() : 0;
            source.setAuxiliarySend(slot, 0);
        }
    }

    private void updateAirAbsorption(float x, float y, float z) {
        if (source == null) return;

        // Music doesn't get muffled by distance
        if (parameters.rank() == Assets.AUDIO_RANK_MUSIC || parameters.rank() == Assets.AUDIO_RANK_NOTIFICATION) {
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
        if (playing) {
            if (source != null) {
                source.stop();
            }
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
        Renderer.getRenderer().getEventQueue().getManager().registerAnimation(this);

        return this;
    }

    @Override
    public final void animate(float t) {
        fadeout_gain -= t * (end_gain / fadeout_time);
        if (fadeout_gain <= 0) {
            stop();
            Renderer.getRenderer().getEventQueue().getManager().removeAnimation(this);
        } else {
            setGain(fadeout_gain);
        }
    }

}
