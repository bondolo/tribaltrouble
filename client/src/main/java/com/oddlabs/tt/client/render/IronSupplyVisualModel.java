package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.effects.particle.RandomVelocityEmitter;
import com.oddlabs.tt.effects.particle.RingEmitter;
import com.oddlabs.tt.effects.render.EmitterAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * {@link VisualModel} implementation for iron supply meteors managing trail, impact explosion, and cooling smoke emitters.
 */
public final class IronSupplyVisualModel extends AbstractVisualModel implements EmitterAccessory {
    private static final float FALL_DURATION_RATIO = 0.12f;
    private static final float SMOKE_PARTICLES_PER_SECOND = 30.0f;
    private static final Color.Linear COLOR_WHITE_HOT = new Color.Linear(2.0f, 2.0f, 2.0f, 1.0f);

    private final IronSupply ironSupply;
    private final AudioImplementation audio;

    private boolean landed = false;
    private boolean cooling = false;
    private boolean airBurstPlayed = false;
    private @Nullable RandomVelocityEmitter trailEmitter = null;
    private @Nullable RandomVelocityEmitter coolingEmitter = null;
    private final Deque<Emitter<?>> oneShotEmitters = new ArrayDeque<>();

    public IronSupplyVisualModel(IronSupply ironSupply, AudioImplementation audio) {
        super(ironSupply);
        this.ironSupply = ironSupply;
        this.audio = audio;
    }

    @Override
    public void animate(float t) {
        if (SupplyVisualState.isSpawning(ironSupply)) {
            float progress = SupplyVisualState.getSpawnProgress(ironSupply);
            if (progress < FALL_DURATION_RATIO) {
                // falling
                float fallProgress = progress / FALL_DURATION_RATIO;
                var emitter = ensureTrailEmitter();

                float pps = 800f + 1600f * fallProgress;
                emitter.setParticlesPerSecond(pps);

                float radiusXZ = 1.6f - 0.8f * fallProgress;
                float radiusY = 2.0f - 1.25f * fallProgress;
                emitter.setParticleRadius(radiusXZ, radiusY, radiusXZ);

                float growthXZ = 2.4f - 1.2f * fallProgress;
                float growthY = 3.0f - 2.25f * fallProgress;
                emitter.setGrowthRate(growthXZ, growthY, growthXZ);

                if (!airBurstPlayed) {
                    airBurstPlayed = true;
                    audio.newAudio(ironSupply.getPositionX(), ironSupply.getPositionY(),
                            ironSupply.getPositionZ(),
                            new AudioParameters(AudioAssets.SFX_LURBLAST, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.8f
                            ));
                }

                float visualZ = ironSupply.getPositionZ() + SupplyVisualState.getOffsetZ(ironSupply);
                emitter.getPosition().set(ironSupply.getPositionX(), ironSupply.getPositionY(), visualZ);
                emitter.animate(t);
            } else {
                if (trailEmitter != null) {
                    trailEmitter.done();
                    oneShotEmitters.add(trailEmitter);
                    trailEmitter = null;
                }

                if (!landed) {
                    landed = true;

                    Vector3f landingPos = new Vector3f(ironSupply.getPositionX(), ironSupply.getPositionY(), ironSupply
                            .getPositionZ() + 0.15f);
                    World world = ironSupply.getWorld();

                    RingEmitter flash = new RingEmitter(
                            world, landingPos, 0.0f,
                            0.0f, 0.0f,
                            1, 1000f,
                            new Vector3f(0f, 0f, 0f),
                            new Vector3f(0f, 0f, 0f),
                            COLOR_WHITE_HOT, Color.LinearDelta.ZERO.alpha(-10.0f),
                            new Vector3f(10.0f, 10.0f, 10.0f),
                            new Vector3f(0.0f, 0.0f, 0.0f),
                            0.1f, 0.0f,
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                            AssetRegistry.getInstance().getSmokeTextures()
                    );
                    oneShotEmitters.add(flash);

                    RingEmitter puff = new RingEmitter(
                            world, landingPos, 0.0f,
                            0.5f, 0.0f,
                            48, 10f,
                            new Vector3f(0f, 0f, 40.0f),
                            new Vector3f(0f, 0f, 0.0f),
                            Color.Linear.WHITE.alpha(0.30f), Color.LinearDelta.ZERO,
                            new Vector3f(1.0f, 1.0f, 0.0f),
                            new Vector3f(15.0f, 15.0f, 0.0f),
                            0.6f, 0.1f,
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                            AssetRegistry.getInstance().getSmokeTextures()
                    );

                    puff.setSpectrumRange(0.0f, 0.9f);
                    puff.setBaseColor(world.getTerrainType().getDustColor());
                    puff.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum * 0.5f));
                    puff.setTransition(0.1f, 0.1f, 0.0f, 0.5f);
                    puff.setJitterIntensity(0.15f);

                    oneShotEmitters.add(puff);

                    RingEmitter debris = new RingEmitter(
                            world, landingPos, 0.0f,
                            0.5f, 0.0f,
                            64, 10f,
                            new Vector3f(0f, 0f, 6.0f),
                            new Vector3f(0f, 0f, 0f),
                            Color.Linear.WHITE.alpha(0.15f), Color.LinearDelta.ZERO,
                            new Vector3f(1.0f, 0.2f, 0.0f),
                            new Vector3f(4.0f, 0.25f, 0.0f),
                            1.0f, 0.45f,
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                            AssetRegistry.getInstance().getSmokeTextures()
                    );
                    debris.setBaseColor(world.getTerrainType().getDustColor());
                    debris.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum
                            * 0.4f));
                    debris.setSpectrumRange(0.1f, 0.9f);
                    debris.setJitterIntensity(0.20f);
                    oneShotEmitters.add(debris);
                }

                float coolProgress = Math.min(1.0f, (progress - FALL_DURATION_RATIO) / (0.85f - FALL_DURATION_RATIO));

                if (!cooling) {
                    cooling = true;
                    audio.newAudio(ironSupply.getPositionX(), ironSupply.getPositionY(),
                            ironSupply.getPositionZ(),
                            new AudioParameters(AudioAssets.SFX_GAS, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.65f
                            )).stop(1.3f);
                }

                var emitter = ensureCoolingEmitter();
                emitter.getPosition().set(ironSupply.getPositionX(), ironSupply.getPositionY(), ironSupply
                        .getPositionZ());
                emitter.setParticlesPerSecond(SMOKE_PARTICLES_PER_SECOND * (1.0f - coolProgress));
                emitter.animate(t);
            }
        } else {
            cleanupEmitters();
        }

        for (var e : oneShotEmitters) {
            e.animate(t);
        }
        oneShotEmitters.removeIf(Emitter::isFinished);
    }

    private void cleanupEmitters() {
        landed = false;
        cooling = false;
        airBurstPlayed = false;
        if (trailEmitter != null) {
            trailEmitter.done();
            trailEmitter = null;
        }
        if (coolingEmitter != null) {
            coolingEmitter.done();
            coolingEmitter = null;
        }
        oneShotEmitters.clear();
    }

    private RandomVelocityEmitter ensureTrailEmitter() {
        if (trailEmitter == null) {
            World world = ironSupply.getWorld();
            float visualZ = ironSupply.getPositionZ() + SupplyVisualState.getOffsetZ(ironSupply);
            Vector3f pos = new Vector3f(ironSupply.getPositionX(), ironSupply.getPositionY(), visualZ);
            trailEmitter = new RandomVelocityEmitter(
                    world, pos, 0.0f, 0.0f,
                    0.02f, 5.0f,
                    0.1f, 0.02f,
                    -1, 800f,
                    new Vector3f(0f, 0f, 10.0f), new Vector3f(0f, 0f, 5.0f),
                    new Color.Linear(0.08f, 0.6f), new Color.LinearDelta(0f, -0.24f),
                    new Vector3f(1.6f, 2.0f, 1.6f), new Vector3f(2.4f, 3.0f, 2.4f),
                    2.5f, 0.1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    AssetRegistry.getInstance().getSmokeTextures(),
                    null, AssetRegistry.getInstance().getSmokeTextures().length,
                    true, true
            );
            trailEmitter.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum));
            trailEmitter.setSpectrumRange(0.2f, 0.2f);
            trailEmitter.setJitterIntensity(0.5f);
            trailEmitter.setEnergyRange(1.2f, 2.5f);
        }
        return trailEmitter;
    }

    private RandomVelocityEmitter ensureCoolingEmitter() {
        if (coolingEmitter == null) {
            World world = ironSupply.getWorld();
            Vector3f pos = new Vector3f(ironSupply.getPositionX(), ironSupply.getPositionY(), ironSupply
                    .getPositionZ());
            coolingEmitter = new RandomVelocityEmitter(
                    world, pos, 0.0f, 0.0f,
                    ironSupply.getSize() * 0.5f, 0.1f, 0.2f, 0.1f,
                    -1, SMOKE_PARTICLES_PER_SECOND,
                    new Vector3f(0f, 0f, 1.5f), new Vector3f(0f, 0f, -0.3f),
                    new Color.Linear(0.15f, 0.5f), new Color.LinearDelta(0f, -0.4f),
                    new Vector3f(0.8f, 0.8f, 0.8f), new Vector3f(2.5f, 2.5f, 2.5f),
                    1.2f, 0.1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    AssetRegistry.getInstance().getSmokeTextures()
            );
            coolingEmitter.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum));
            coolingEmitter.setSpectrumRange(0.2f, 0.8f);
            coolingEmitter.setJitterIntensity(0.02f);
            coolingEmitter.setTransition(0f, 3.0f, 0.8f, 0f);
        }
        return coolingEmitter;
    }

    @Override
    public void addEmitters(Collection<Emitter<?>> dest) {
        if (trailEmitter != null) {
            dest.add(trailEmitter);
        }
        if (coolingEmitter != null) {
            dest.add(coolingEmitter);
        }
        dest.addAll(oneShotEmitters);
    }

    @Override
    protected boolean isSelfExpired() {
        return ironSupply.isDead();
    }

    @Override
    public boolean isVisible(Model parent, CameraState camera) {
        return !ironSupply.isDead();
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
        cleanupEmitters();
    }
}
