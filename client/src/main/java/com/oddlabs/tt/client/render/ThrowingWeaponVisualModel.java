package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.simulation.model.weapon.ThrowingWeapon;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Visual model for throwing weapons managing spatial flight audio, rotation spin, and visual loft.
 */
public final class ThrowingWeaponVisualModel extends AbstractVisualModel implements AnimatedAccessory {
    private static final float GRAVITY_MAGNITUDE = 3.0f * 9.82f;
    private static final float SPEAR_LOFT_FACTOR = 1.05f;
    private static final float AXE_LOFT_FACTOR = 1.01f;
    private static final float INITIAL_AXE_ROTATION = (float) Math.toRadians(345.0);
    private static final float SPEAR_ROLL = (float) Math.PI;

    private final ThrowingWeapon weapon;
    private final @Nullable AudioPlayer audioPlayer;
    private final float effectiveGravity;
    private final float angleVelocity;

    public ThrowingWeaponVisualModel(ThrowingWeapon weapon, AudioImplementation audio) {
        super(weapon);
        this.weapon = weapon;
        boolean isDirected = weapon instanceof DirectedThrowingWeapon;
        float loft = isDirected ? SPEAR_LOFT_FACTOR : AXE_LOFT_FACTOR;
        this.effectiveGravity = GRAVITY_MAGNITUDE * loft * loft;
        float rotsPerSec = isDirected ? 0f : switch (weapon.getWeaponVisualType()) {
            case ROCK -> 3f;
            case IRON -> 6f;
            case RUBBER -> 9f;
            case SONIC_BLAST -> 0f;
        };
        this.angleVelocity = (float) (rotsPerSec * 2.0 * Math.PI);

        var sound = isDirected
                ? AudioRegistry.SFX_WEAPON_SPEAR
                : AudioRegistry.SFX_WEAPON_AXE;
        var params = new AudioParameters(sound, AudioRegistry.AUDIO_RANK_WEAPON_ATTACK,
                AudioRegistry.AUDIO_DISTANCE_WEAPON_ATTACK, AudioRegistry.AUDIO_GAIN_WEAPON_ATTACK,
                AudioRegistry.AUDIO_RADIUS_WEAPON_ATTACK,
                ThreadLocalRandom.current().nextFloat(0.9f, 1.1f));
        this.audioPlayer = audio.newAudio(weapon.getPositionX(), weapon.getPositionY(), weapon.getPositionZ(), params);
    }

    private float computeVisualZ() {
        float t = weapon.getTime();
        float totalTime = weapon.getTimeLimit();
        if (totalTime > 0f && t < totalTime) {
            return weapon.getPositionZ() + 0.5f * effectiveGravity * t * (totalTime - t);
        }
        return weapon.getPositionZ();
    }

    @Override
    public void animate(float dt) {
        if (audioPlayer != null) {
            audioPlayer.setPosition(weapon.getPositionX(), weapon.getPositionY(), computeVisualZ());
        }
    }

    public void getTransform(Matrix4f dest) {
        float t = weapon.getTime();
        float totalTime = weapon.getTimeLimit();
        float visualZ;
        float pitchRad;

        float baselineVz = totalTime > 0f ? (weapon.getDestZ() - weapon.getStartZ()) / totalTime : 0f;
        if (totalTime > 0f && t < totalTime) {
            visualZ = weapon.getPositionZ() + 0.5f * effectiveGravity * t * (totalTime - t);
            float loftVz = 0.5f * effectiveGravity * (totalTime - 2f * t);
            pitchRad = (float) Math.atan2(baselineVz + loftVz, weapon.getMetersPerSecond());
        } else {
            visualZ = weapon.getPositionZ();
            pitchRad = (float) Math.atan2(baselineVz, weapon.getMetersPerSecond());
        }

        float yawRad = (float) Math.atan2(weapon.getDirectionY(), weapon.getDirectionX());
        dest.translation(weapon.getPositionX(), weapon.getPositionY(), visualZ)
                .rotate(yawRad, 0f, 0f, 1f);

        if (weapon instanceof DirectedThrowingWeapon) {
            dest.rotate(-pitchRad, 0f, 1f, 0f)
                    .rotate(SPEAR_ROLL, 1f, 0f, 0f);
        } else {
            dest.rotate(INITIAL_AXE_ROTATION + angleVelocity * t, 0f, 1f, 0f);
        }
    }

    @Override
    protected boolean isSelfExpired() {
        return weapon.isDead();
    }

    @Override
    public boolean isVisible(Model parent, CameraState camera) {
        return !weapon.isDead();
    }

    @Override
    public void getRelativeTransform(Matrix4f dest, Model parent) {
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    @Override
    public void close() {
        super.close();
        if (audioPlayer != null) {
            audioPlayer.stop();
        }
    }
}
