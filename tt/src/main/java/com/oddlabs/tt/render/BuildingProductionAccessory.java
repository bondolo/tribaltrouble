package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.particle.LinearEmitter;
import com.oddlabs.tt.particle.RandomAccelerationEmitter;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * An accessory that manages the chimney smoke during building production.
 */
public final class BuildingProductionAccessory implements AnimatedAccessory {
    private static final float EMITTER_ENERGY = 5.0f;
    private static final float EMITTER_ALPHA = 0.75f;
    private static final float EMITTER_RADIUS_XY = 0.01f;
    private static final float EMITTER_HEIGHT = 1.5f;
    private static final float ACCELERATION_FACTOR = 0.1f;
    private static final float PARTICLES_PER_SECOND = 15.0f;
    private static final float IDLE_STOP_THRESHOLD = 0.3f;

    private static final Vector3fc ZERO_VEC = new Vector3f(0f, 0f, 0f);
    private static final Vector3fc EMITTER_ACCEL = new Vector3f(0f, 0f, 1.3f);
    private static final float PARTICLE_DAMPING = 0.7f;
    private static final Color PARTICLE_COLOR = new Color.Standard(0xBF_59_59_59);
    private static final Vector3fc PARTICLE_RADIUS = new Vector3f(0.3f, 0.3f, 0.3f);
    private static final Vector3fc PARTICLE_GROWTH = new Vector3f(0.5f, 0.5f, 0.5f);

    private final @NonNull LinearEmitter emitter;
    private final @NonNull Vector3fc chimneyOffset;
    private float idleTimer = 0f;

    public BuildingProductionAccessory(@NonNull Building building, @NonNull Vector3fc chimneyOffset,
            @NonNull TextureKey @NonNull [] textures) {
        this.chimneyOffset = chimneyOffset;

        this.emitter = new RandomAccelerationEmitter(building.getOwner().getWorld(), new Vector3f(0f, 0f, 0f), 0f,
                EMITTER_RADIUS_XY, EMITTER_RADIUS_XY, EMITTER_HEIGHT, ACCELERATION_FACTOR,
                -1, PARTICLES_PER_SECOND,
                ZERO_VEC, EMITTER_ACCEL, ACCELERATION_FACTOR,
                PARTICLE_COLOR, new Color.Linear(0f, 0f, 0f, -EMITTER_ALPHA / EMITTER_ENERGY),
                PARTICLE_RADIUS, PARTICLE_GROWTH, EMITTER_ENERGY, PARTICLE_DAMPING,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                textures, null, textures.length);
        this.emitter.stop();
    }

    @Override
    public void animate(float t) {
        emitter.animate(t);
        idleTimer += t;
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        if (parent instanceof Building building) {
            // Only armories have production smoke
            if (!building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
                emitter.stop();
                return !emitter.isFinished();
            }

            boolean isProducing = building.isAlive() && building.isProducing();
            if (isProducing) {
                emitter.start();
                idleTimer = 0f;
            } else {
                // Only stop emitting if we've been idle for a while (handles WeaponsProducer breaks)
                if (idleTimer > IDLE_STOP_THRESHOLD) {
                    emitter.stop();
                }
            }

            return isProducing || !emitter.isFinished();
        }
        return false;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
        dest.translate(chimneyOffset);
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    public @NonNull LinearEmitter getEmitter() {
        return emitter;
    }
}
