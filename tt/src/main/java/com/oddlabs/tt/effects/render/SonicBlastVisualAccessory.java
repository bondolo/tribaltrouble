package com.oddlabs.tt.effects.render;

import com.oddlabs.tt.client.render.*;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.engine.audio.AudioPlayer;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.SonicBlast;
import com.oddlabs.tt.effects.particle.SonicBlastEffect;
import com.oddlabs.tt.engine.resource.AudioAssets;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Client-side visual accessory for the sonic blast spell.
 * Manages the expanding ring visual effect and cast audio entirely on the client.
 */
public final class SonicBlastVisualAccessory implements AnimatedAccessory {
    private final @NonNull SonicBlast blast;
    private @Nullable SonicBlastEffect effect;
    private final @NonNull AudioPlayer lur;
    private final @NonNull AudioPlayer rumble;

    public SonicBlastVisualAccessory(@NonNull SonicBlast blast) {
        this.blast = blast;
        World world = blast.getWorld();
        this.lur = world.getAudio().newAudio(blast.getPositionX(), blast.getPositionY(), blast.getPositionZ(),
                AudioAssets.SONIC_BLAST_LUR[world.getRandom().nextInt(AudioAssets.SONIC_BLAST_LUR.length)]);
        this.rumble = world.getAudio().newAudio(blast.getPositionX(), blast.getPositionY(), blast.getPositionZ(),
                AudioAssets.SONIC_BLAST_RUMBLE);
    }

    /**
     * Triggers the expanding shockwave visual effect and impact audio.
     *
     * @param x Center X coordinate
     * @param y Center Y coordinate
     * @param z Center Z coordinate
     * @param radius Shockwave maximum radius
     * @param duration Duration in seconds
     */
    public void triggerBlast(float x, float y, float z, float radius, float duration) {
        if (effect != null) {
            effect.abort();
        }
        World world = blast.getWorld();
        this.effect = new SonicBlastEffect(world, new Vector3f(x, y, z), radius, duration);
        world.getAudio().newAudio(x, y, z, AudioAssets.SONIC_BLAST);
        if (lur != null) {
            lur.stop(10.0f);
        }
        if (rumble != null) {
            rumble.stop(15.0f);
        }
    }

    @Override
    public void animate(float t) {
    }

    public @Nullable SonicBlastEffect getEffect() {
        return effect;
    }

    @Override
    public boolean isExpired() {
        return blast.isDead();
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        return !blast.isDead();
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    @Override
    public void close() {
        if (effect != null) {
            effect.abort();
        }
        if (lur != null) {
            lur.stop(15.0f);
        }
        if (rumble != null) {
            rumble.stop(15.0f);
        }
    }
}
