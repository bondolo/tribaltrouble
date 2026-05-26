package com.oddlabs.tt.model;

import com.oddlabs.tt.particle.LinearEmitter;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * An accessory that manages the damage smoke for a building.
 * Dynamically scales its effect based on the parent building's health.
 */
public final class BuildingDamagedAccessory implements AnimatedAccessory {
    private static final float INITIAL_PARTICLE_ALPHA = 3.0f;
    private static final float MIN_EMITTER_ENERGY = 3.0f;
    private static final float MAX_EMITTER_ENERGY = 5.0f;

    private static final float EMITTER_RADIUS_XY = 0.01f;
    private static final float EMITTER_HEIGHT = 0.5f;
    private static final float SPREAD_FACTOR = 0.7f;
    private static final float PARTICLES_PER_SECOND = 25.0f;

    private static final Vector3fc ZERO_VEC = new Vector3f(0f, 0f, 0f);
    private static final Vector3fc PARTICLE_VELOCITY = new Vector3f(0f, 0f, 5f);
    private static final Color PARTICLE_BASE_COLOR = new Color.Linear(new Color.Standard(0.3f, 0.3f, 0.3f,
            INITIAL_PARTICLE_ALPHA));
    private static final Vector3fc PARTICLE_RADIUS = new Vector3f(1.5f, 1.5f, 1.5f);
    private static final Vector3fc PARTICLE_GROWTH = new Vector3f(0.6f, 0.6f, 0.6f);

    private final @NonNull LinearEmitter emitter;
    private final float hitOffsetZ;

    public BuildingDamagedAccessory(@NonNull Building building, float hitOffsetZ,
            @NonNull TextureKey @NonNull [] textures) {
        this.hitOffsetZ = hitOffsetZ;
        this.emitter = new RandomVelocityEmitter(building.getOwner().getWorld(), new Vector3f(0f, 0f, 0f), 0f, 0f,
                EMITTER_RADIUS_XY, EMITTER_RADIUS_XY, EMITTER_HEIGHT, SPREAD_FACTOR,
                -1, PARTICLES_PER_SECOND,
                PARTICLE_VELOCITY, ZERO_VEC,
                PARTICLE_BASE_COLOR, Color.Standard.TRANSPARENT,
                PARTICLE_RADIUS, PARTICLE_GROWTH, 1.5f, .75f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                textures);
        this.emitter.stop();
    }

    @Override
    public void animate(float t) {
        emitter.animate(t);
    }

    @Override
    public boolean isVisible(@NonNull AccessorizableModel parent) {
        if (parent instanceof Building building) {
            int startSmoke = building.getTemplate().getMaxHitPoints() / 2;
            int hp = building.getHitPoints();
            boolean isDamaged = !building.isDead() && building.isComplete() && hp < startSmoke;

            if (isDamaged) {
                float energy = MIN_EMITTER_ENERGY + ((1 - (float) hp / startSmoke) * (MAX_EMITTER_ENERGY
                        - MIN_EMITTER_ENERGY));
                emitter.start();
                emitter.setDeltaColor(new Color.LinearDelta(0f, 0f, 0f, -INITIAL_PARTICLE_ALPHA / energy));
                emitter.setEnergy(energy);
            } else {
                emitter.stop();
            }

            return isDamaged || !emitter.isFinished();
        }
        return false;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull AccessorizableModel parent) {
        dest.translate(0f, 0f, hitOffsetZ);
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    public @NonNull LinearEmitter getEmitter() {
        return emitter;
    }
}
