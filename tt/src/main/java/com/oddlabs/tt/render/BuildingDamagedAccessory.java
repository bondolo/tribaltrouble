package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.particle.LinearEmitter;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.util.Color;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * An accessory that manages the damage smoke for a building.
 * Dynamically scales its effect based on the parent building's health.
 */
public final class BuildingDamagedAccessory implements AnimatedAccessory {
    private static final float INITIAL_PARTICLE_ALPHA = 0.8f;
    private static final float MIN_EMITTER_ENERGY = 3.0f;
    private static final float MAX_EMITTER_ENERGY = 5.0f;
    private static final float MIN_EMITTER_PPS = 15.0f;
    private static final float MAX_EMITTER_PPS = 50.0f;
    private static final float MIN_EMITTER_JITTER = 0.02f;
    private static final float MAX_EMITTER_JITTER = 0.15f;
    private static final float MIN_EMITTER_SCALE = 0.9f;
    private static final float MAX_EMITTER_SCALE = 1.3f;

    private static final Color.Linear FACTOR_START = new Color.Linear(1.5f, 0.6f);
    private static final Color.Linear FACTOR_END = new Color.Linear(0.3f, 1.0f);
    private static final Color.LinearDelta SOOT_DELTA = Color.LinearDelta.red(0.05f);
    private static final Color.Linear SOOT_TINT = new Color.Linear(1.0f, 0.9f, 0.7f, 1.0f);

    private final @NonNull Building building;
    private final @NonNull LinearEmitter emitter;
    private final float hitOffsetZ;

    public BuildingDamagedAccessory(@NonNull Building building, float hitOffsetZ,
            @NonNull TextureKey @NonNull [] textures) {
        this.building = building;
        this.hitOffsetZ = hitOffsetZ;
        this.emitter = new RandomVelocityEmitter(building.getOwner().getWorld(), new Vector3f(0f, 0f, 0f), 0f, 0f,
                0.01f, 0.1f, 0.5f, 0.7f, -1, 25.0f,
                new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, 0f),
                Color.Linear.WHITE, Color.LinearDelta.ZERO,
                new Vector3f(1.5f, 1.5f, 1.5f), new Vector3f(0.6f, 0.6f, 0.6f), 1.5f, .75f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, textures);
        this.emitter.stop();
        this.emitter.setBaseColor(new Color.Standard(0.3f, INITIAL_PARTICLE_ALPHA).linear());
        this.emitter.setSpectrumRange(0.0f, 1.0f);
        this.emitter.setJitterIntensity(MIN_EMITTER_JITTER);

        this.emitter.setColorSpectrum((spectrum, baseColor) -> {
            Random rand = ThreadLocalRandom.current();
            Color.Linear factor = FACTOR_START.lerp(FACTOR_END, spectrum);
            Color.Linear grayColor = baseColor.mul(factor);

            if (rand.nextFloat() < 0.6f) {
                return grayColor;
            } else {
                Color.LinearDelta sootDelta = SOOT_DELTA.mul(spectrum);
                return grayColor.mul(SOOT_TINT).add(sootDelta);
            }
        });
    }

    @Override
    public void animate(float t) {
        Building.BuildStage stage = building.getBuildStage();
        boolean isCompleteOrHalfBuilt = stage == Building.BuildStage.HALFBUILT || stage == Building.BuildStage.BUILT;

        int hp = building.getHitPoints();
        float damageThreshold = building.getBuildPoints() / 2.0f;
        boolean isDamaged = building.isAlive() && isCompleteOrHalfBuilt && hp < damageThreshold;

        if (isDamaged) {
            float damageFactor = Math.clamp((damageThreshold - hp) / damageThreshold, 0.0f, 1.0f);

            float energy = MIN_EMITTER_ENERGY + damageFactor * (MAX_EMITTER_ENERGY - MIN_EMITTER_ENERGY);
            emitter.setEnergy(energy);

            float pps = MIN_EMITTER_PPS + damageFactor * (MAX_EMITTER_PPS - MIN_EMITTER_PPS);
            emitter.setParticlesPerSecond(pps);

            float scale = MIN_EMITTER_SCALE + damageFactor * (MAX_EMITTER_SCALE - MIN_EMITTER_SCALE);
            emitter.scale(scale, scale, scale);

            emitter.setSpectrum(damageFactor);

            float jitter = MIN_EMITTER_JITTER + damageFactor * (MAX_EMITTER_JITTER - MIN_EMITTER_JITTER);
            emitter.setJitterIntensity(jitter);

            emitter.start();
        } else {
            emitter.stop();
        }

        emitter.animate(t);
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        if (parent instanceof Building b) {
            Building.BuildStage stage = b.getBuildStage();
            boolean isCompleteOrHalfBuilt = stage == Building.BuildStage.HALFBUILT || stage
                    == Building.BuildStage.BUILT;
            int hp = b.getHitPoints();
            float damageThreshold = b.getBuildPoints() / 2.0f;
            boolean isDamaged = b.isAlive() && isCompleteOrHalfBuilt && hp < damageThreshold;

            return isDamaged || emitter.hasActiveParticles();
        }
        return false;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
        float z = parent instanceof Building b ? b.getHitOffsetZ() : hitOffsetZ;
        dest.translate(0f, 0f, z);
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    public @NonNull LinearEmitter getEmitter() {
        return emitter;
    }
}
