package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.ThrowingWeapon;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract visual model for throwing weapons managing spatial flight audio and visual ballistic loft.
 *
 * @param <T> weapon type
 */
abstract sealed class ThrowingWeaponVisualModel<T extends ThrowingWeapon> extends AbstractVisualModel
        implements AnimatedAccessory permits DirectedThrowingWeaponVisualModel, RotatingThrowingWeaponVisualModel {
    protected static final float GRAVITY_MAGNITUDE = 3.0f * 9.82f;

    protected final T weapon;
    private final @Nullable AudioPlayer audioPlayer;
    protected final float effectiveGravity;

    ThrowingWeaponVisualModel(T weapon, AudioImplementation audio, float loftFactor, AudioFile sound) {
        super(weapon);
        this.weapon = weapon;
        this.effectiveGravity = GRAVITY_MAGNITUDE * loftFactor * loftFactor;

        var params = new AudioParameters(sound, AudioRegistry.AUDIO_RANK_WEAPON_ATTACK,
                AudioRegistry.AUDIO_DISTANCE_WEAPON_ATTACK, AudioRegistry.AUDIO_GAIN_WEAPON_ATTACK,
                AudioRegistry.AUDIO_RADIUS_WEAPON_ATTACK,
                ThreadLocalRandom.current().nextFloat(0.9f, 1.1f));
        this.audioPlayer = audio.newAudio(weapon.getPositionX(), weapon.getPositionY(), weapon.getPositionZ(), params);
    }

    protected final float computeVisualZ() {
        float t = weapon.getTime();
        float totalTime = weapon.getTimeLimit();
        if (totalTime > 0f && t < totalTime && effectiveGravity > 0f) {
            return weapon.getPositionZ() + 0.5f * effectiveGravity * t * (totalTime - t);
        }
        return weapon.getPositionZ();
    }

    @Override
    public final void animate(float dt) {
        if (audioPlayer != null) {
            audioPlayer.setPosition(weapon.getPositionX(), weapon.getPositionY(), computeVisualZ());
        }
    }

    public abstract void getTransform(Matrix4f dest);

    @Override
    protected final boolean isSelfExpired() {
        return weapon.isDead();
    }

    @Override
    public final boolean isVisible(Model parent, CameraState camera) {
        return !weapon.isDead();
    }

    @Override
    public final void close() {
        super.close();
        if (audioPlayer != null) {
            audioPlayer.stop();
        }
    }
}
