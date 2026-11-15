package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.AudioSettings;
import com.oddlabs.tt.audio.ReverbType;
import com.oddlabs.tt.base.animation.AnimationManager;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.URL;

/**
 * A silent {@link AudioManager} that wraps a delegate (normally an {@link OpenALManager}) and forces all output to
 * zero gain while the delegate still opens a real device and runs the real source/buffer lifecycle underneath. This is
 * the "mock device that forces gain to zero" used by the lwjglx diagnostic posture: it lets the agent trace every
 * {@code alSource*} / {@code alDeleteSources} call on a working context (so use-after-free and double-delete are still
 * caught) without emitting audible output, avoiding the also-soft null-driver path that aborts this platform's
 * OpenAL-Soft natively on teardown (see {@code docs/openal_lifecycle_architecture.md} §4).
 *
 * <p>The game's existing focus fade (which toggles master gain between 1 and 0 in {@code Peer}) runs unchanged: a
 * stored gain is tracked and reported by {@link #getMasterGain()} so callers that read it back still see a non-zero
 * value, while the delegate is always told the master gain is zero. No gameplay observable changes except that this
 * manager makes no sound.</p>
 */
final class SilentAudioManager implements AudioManager, AutoCloseable {
    private static final float SILENT_GAIN = 0f;

    @NonNull
    private final AudioManager delegate;
    private float masterGain = 1f;

    SilentAudioManager(AudioSettings settings, AnimationManager animationManager) {
        this.delegate = new OpenALManager(settings, animationManager);
    }

    @Override
    public AudioPlayer newAudio(float x, float y, float z, AudioParameters params) {
        return delegate.newAudio(x, y, z, params);
    }

    @Override
    public void update(float dt) {
        delegate.update(dt);
    }

    @Override
    public AudioManager startSources() {
        return delegate.startSources();
    }

    @Override
    public AudioManager stopSources() {
        return delegate.stopSources();
    }

    @Override
    public Vector3fc getListenerPosition() {
        return delegate.getListenerPosition();
    }

    // Report the tracked gain, not SILENT_GAIN, so callers that read it back (e.g. a focus fade in Peer) still see a
    // non-zero value and run their logic unchanged; only the delegate actually drives the output gain.
    @Override
    public float getMasterGain() {
        return masterGain;
    }

    @Override
    public float getMusicGain() {
        return delegate.getMusicGain();
    }

    @Override
    public float getSfxGain() {
        return delegate.getSfxGain();
    }

    @Override
    public boolean isEFXSupported() {
        return delegate.isEFXSupported();
    }

    @Override
    public boolean isHRTFSupported() {
        return delegate.isHRTFSupported();
    }

    @Override
    public boolean isSfxEnabled() {
        return delegate.isSfxEnabled();
    }

    // Track the requested gain for reporting but force zero on the delegate so the game stays observable-silent.
    @Override
    public AudioManager setMasterGain(float gain) {
        this.masterGain = gain;
        delegate.setMasterGain(SILENT_GAIN);
        return this;
    }

    @Override
    public AudioManager setMusicGain(float gain) {
        return delegate.setMusicGain(gain);
    }

    @Override
    public AudioManager setSfxGain(float gain) {
        return delegate.setSfxGain(gain);
    }

    @Override
    public AudioManager setHeadphoneMode(boolean enabled) {
        return delegate.setHeadphoneMode(enabled);
    }

    @Override
    public AudioManager setListenerOrientation(Vector3fc forward, Vector3fc up) {
        return delegate.setListenerOrientation(forward, up);
    }

    @Override
    public AudioManager setListenerPosition(float x, float y, float z) {
        return delegate.setListenerPosition(x, y, z);
    }

    @Override
    public AudioManager setSfxEnabled(boolean enabled) {
        return delegate.setSfxEnabled(enabled);
    }

    @Override
    public boolean startPlaying() {
        return delegate.startPlaying();
    }

    @Override
    public Audio createAudio(URL file) throws IOException {
        return delegate.createAudio(file);
    }

    @Override
    public void toggleMusic() {
        delegate.toggleMusic();
    }

    @Override
    public void setMusicEnabled(boolean enabled) {
        delegate.setMusicEnabled(enabled);
    }

    @Override
    public boolean isMusicEnabled() {
        return delegate.isMusicEnabled();
    }

    @Override
    public void setMusic(AudioParameters musicAudio, float delay) {
        delegate.setMusic(musicAudio, delay);
    }

    @Override
    public AudioPlayer getMusicPlayer() {
        return delegate.getMusicPlayer();
    }

    @Override
    public void stopMusic() {
        delegate.stopMusic();
    }

    @Override
    public void stopMusic(float decayRate) {
        delegate.stopMusic(decayRate);
    }

    @Override
    public void setReverb(ReverbType type) {
        delegate.setReverb(type);
    }

    @Override
    public void setReverb(ReverbType from, ReverbType to, float factor) {
        delegate.setReverb(from, to, factor);
    }

    @Override
    public void close() {
        try {
            delegate.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close silent audio delegate", e);
        }
    }
}
