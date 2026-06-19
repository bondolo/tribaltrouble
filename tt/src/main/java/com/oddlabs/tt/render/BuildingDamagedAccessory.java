package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Terrain;
import com.oddlabs.tt.landscape.procedural.Landscape;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.render.particle.ColorSpectrum;
import com.oddlabs.tt.render.particle.LinearEmitter;
import com.oddlabs.tt.render.particle.RandomVelocityEmitter;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import com.oddlabs.tt.model.snapshot.VisualSnapshots;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;

/**
 * An accessory that manages the damage smoke for a building.
 * Dynamically scales its effect based on the parent building's health.
 */
public final class BuildingDamagedAccessory implements EmitterAccessory {
    private static final Color.Linear PARTICULATE_COLOR = new Color.Standard(0.08f, 0.06f, 0.05f, 0.8f).linear();
    private static final Color.Linear DAMAGE_BASE_COLOR = new Color.Standard(0.3f, 0.8f).linear();
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
    private boolean hasCollapsed = false;

    public BuildingDamagedAccessory(@NonNull Building building, float hitOffsetZ) {
        this.building = building;
        this.hitOffsetZ = hitOffsetZ;
        this.emitter = new RandomVelocityEmitter(building.getOwner().getWorld(), new Vector3f(0f, 0f, 0f), 0f, 0f,
                0.01f, 0.1f, 0.5f, 0.7f, -1, 25.0f,
                new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, 0f),
                Color.Linear.WHITE, Color.LinearDelta.ZERO,
                new Vector3f(1.5f, 1.5f, 1.5f), new Vector3f(0.6f, 0.6f, 0.6f), 1.5f, .75f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                VisualRegistry.getInstance().getDamageSmokeTextures());
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

        if (isDamaged || emitter.hasActiveParticles()) {
            emitter.animate(t);
        }

        if (building.isDead() && !hasCollapsed) {
            hasCollapsed = true;
            triggerCollapseEffects();
        }
    }

    private void triggerCollapseEffects() {
        building.getOwner().getWorld().getAudio().newAudio(building.getPositionX(), building.getPositionY(),
                building.getPositionZ(), AudioAssets.BUILDING_COLLAPSE);
        final Terrain terrain = building.getOwner().getWorld().getTerrainType();
        final Color.Linear dustColor = Landscape.getDustColor(terrain).desaturate(0.5f);

        ColorSpectrum spectrumCallback = (spectrum, baseColor) -> {
            Random rand = ThreadLocalRandom.current();
            Color.Linear baseDustColor = (rand.nextFloat() < 0.25f) ? PARTICULATE_COLOR : dustColor;

            if (spectrum < 0.15f) {
                Color.Linear grayColor = DAMAGE_BASE_COLOR.mul(FACTOR_END);
                return (rand.nextFloat() < 0.6f) ? grayColor : grayColor.mul(SOOT_TINT).add(SOOT_DELTA);
            } else if (spectrum < 0.25f) {
                float t1 = (spectrum - 0.15f) / 0.10f;
                Color.Linear grayColor = DAMAGE_BASE_COLOR.mul(FACTOR_END);
                Color.Linear smokeColor = (rand.nextFloat() < 0.6f) ? grayColor : grayColor.mul(SOOT_TINT).add(
                        SOOT_DELTA);
                return smokeColor.lerp(baseDustColor, t1);
            } else if (spectrum < 0.67f) {
                return baseDustColor;
            } else {
                float fade = Math.clamp((1.0f - spectrum) / 0.33f, 0.0f, 1.0f);
                return baseDustColor.alpha(baseDustColor.a() * fade);
            }
        };

        var buildingVisuals = VisualRegistry.getInstance()
                .getBuildingVisuals(building.getOwner().getRaceInfo().getRaceType(), building.getTemplate()
                        .getBuildingType());

        RandomVelocityEmitter collapse_emitter = new RandomVelocityEmitter(building.getOwner().getWorld(), new Vector3f(
                building.getPositionX(), building.getPositionY(), building.getPositionZ()), 0f, 0f,
                buildingVisuals.smokeRadius(), buildingVisuals.smokeHeight(), 1f, 1f,
                120, 80f,
                new Vector3f(0f, 0f, .1f), new Vector3f(0f, 0f, -2.5f),
                Color.Linear.WHITE, Color.LinearDelta.ZERO.alpha(-1f),
                new Vector3f(1f, 1f, 1f), new Vector3f(7.5f, 7.5f, 7.5f), 1.2f, 0.75f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                VisualRegistry.getInstance().getSmokeTextures());
        collapse_emitter.setColorSpectrum(spectrumCallback);

        building.getOwner().getWorld().getNotificationListener().emitterSpawned(collapse_emitter, true);

        {
            float energy = 3f;
            float fade_speed = 2.5f;

            RandomVelocityEmitter fragments_emitter = new RandomVelocityEmitter(building.getOwner().getWorld(),
                    new Vector3f(
                            building.getPositionX(), building.getPositionY(), building.getPositionZ()), 0f,
                    buildingVisuals.smokeRadius(), buildingVisuals.smokeHeight(), 0.5f,
                    (float) Math.PI,
                    buildingVisuals.numFragments(), buildingVisuals.numFragments(),
                    new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, -25f),
                    Color.Linear.WHITE.alpha(energy * fade_speed), Color.LinearDelta.ZERO.alpha(-fade_speed),
                    new Vector3f(1f, 1f, 1f), new Vector3f(0f, 0f, 0f), energy, .75f,
                    VisualRegistry.getInstance().getWoodFragments(),
                    true, true);
            building.getOwner().getWorld().getNotificationListener().emitterSpawned(fragments_emitter, false);
        }

        {
            float energy = 3f;
            float fade_speed = 2.5f;

            RandomVelocityEmitter fragments_emitter = new RandomVelocityEmitter(building.getOwner().getWorld(),
                    new Vector3f(
                            building.getPositionX(), building.getPositionY(), building.getPositionZ()), 0f,
                    buildingVisuals.smokeRadius(), buildingVisuals.smokeHeight(), 0.5f,
                    (float) Math.PI,
                    buildingVisuals.numFragments(), buildingVisuals.numFragments(),
                    new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, -25f),
                    new Color.Linear(1f, 1f, 1f, energy * fade_speed), new Color.LinearDelta(0f, 0f, 0f,
                            -fade_speed),
                    new Vector3f(1f, 1f, 1f), new Vector3f(0f, 0f, 0f), energy, .75f,
                    VisualRegistry.getInstance().getWoodFragments(),
                    true, true);
            building.getOwner().getWorld().getNotificationListener().emitterSpawned(fragments_emitter, false);
        }
    }

    @Override
    public boolean isVisible(@NonNull EntitySnapshot parent, @NonNull CameraState camera) {
        if (parent instanceof VisualSnapshots.BuildingSnapshot) {
            Building.BuildStage stage = building.getBuildStage();
            boolean isCompleteOrHalfBuilt = stage == Building.BuildStage.HALFBUILT || stage
                    == Building.BuildStage.BUILT;
            int hp = building.getHitPoints();
            float damageThreshold = building.getBuildPoints() / 2.0f;
            boolean isDamaged = building.isAlive() && isCompleteOrHalfBuilt && hp < damageThreshold;

            return isDamaged || emitter.hasActiveParticles();
        }
        return false;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull EntitySnapshot parent) {
        float z = parent instanceof VisualSnapshots.BuildingSnapshot ? building.getHitOffsetZ() : hitOffsetZ;
        dest.translate(0f, 0f, z);
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    @Override
    public @NonNull LinearEmitter getEmitter() {
        return emitter;
    }
}
