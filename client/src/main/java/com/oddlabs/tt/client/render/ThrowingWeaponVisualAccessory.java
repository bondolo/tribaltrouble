package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.simulation.model.weapon.ThrowingWeapon;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Presentation accessory that manages flight audio for throwing weapons.
 */
public final class ThrowingWeaponVisualAccessory implements AnimatedAccessory {
    private final ThrowingWeapon weapon;
    private final @Nullable AudioPlayer audioPlayer;

    public ThrowingWeaponVisualAccessory(ThrowingWeapon weapon, AudioImplementation audio) {
        this.weapon = weapon;
        var sound = weapon instanceof DirectedThrowingWeapon
                ? AudioAssets.SFX_WEAPON_SPEAR
                : AudioAssets.SFX_WEAPON_AXE;
        var params = new AudioParameters(sound, AudioAssets.AUDIO_RANK_WEAPON_ATTACK,
                AudioAssets.AUDIO_DISTANCE_WEAPON_ATTACK, AudioAssets.AUDIO_GAIN_WEAPON_ATTACK,
                AudioAssets.AUDIO_RADIUS_WEAPON_ATTACK,
                ThreadLocalRandom.current().nextFloat(0.9f, 1.1f));
        this.audioPlayer = audio.newAudio(weapon.getPositionX(), weapon.getPositionY(), weapon.getPositionZ(), params);
    }

    @Override
    public void animate(float t) {
        if (audioPlayer != null) {
            audioPlayer.setPosition(weapon.getPositionX(), weapon.getPositionY(), weapon.getPositionZ());
        }
    }

    @Override
    public boolean isExpired() {
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
        if (audioPlayer != null) {
            audioPlayer.stop();
        }
    }
}
