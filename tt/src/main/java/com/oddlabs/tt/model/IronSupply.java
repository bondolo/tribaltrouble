package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.landscape.World;
import java.util.concurrent.ThreadLocalRandom;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.particle.RingEmitter;
import com.oddlabs.tt.particle.SonicBlastEffect;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.LandscapeResources;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * An iron boulder supply that falls from the sky as a meteor.
 */
public final class IronSupply extends SupplyModel {
    private static final int INITIAL_SUPPLIES = 10;
    private static final float SPAWN_OFFSET_Z = 75.0f;
    private static final float FALL_DURATION_RATIO = 0.12f;

    private static final Color.Linear COLOR_FALLING = new Color.Standard(0xFF_FF_CC_00).linear();
    private static final Color.Linear COLOR_RED_HOT = new Color.Standard(0xFF_FF_00_00).linear();
    private static final Color.Linear COLOR_COOLING = new Color.Standard(0xFF_6B_6B_7A).linear();
    private static final Color.Linear COLOR_DECAL_COOLED = new Color.Standard(0xFF_00_00_00).linear();

    private Color.@Nullable Linear spawnColorTint = COLOR_FALLING;
    private Color.@Nullable Linear crackDecalColor = null;
    private float crackDecalOpacity = 0.0f;
    private float crackDecalDiameter = 0.0f;
    private float crackDecalPattern = 0.0f;
    private @Nullable PointEmitterModel trailEmitter = null;
    private @Nullable PointEmitterModel coolingEmitter = null;
    private boolean landed = false;
    private boolean cooling = false;

    public IronSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        var rotation = ThreadLocalRandom.current().nextFloat((float) -Math.PI, (float) Math.PI);
        var fragmentIndex = ThreadLocalRandom.current().nextInt(LandscapeResources.SUPPLY_FRAGMENT_COUNT);
        super(world, 2f, grid_x, grid_y, x, y, SPAWN_OFFSET_Z, rotation, INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getIronBounds(fragmentIndex));
    }

    @Override
    public @NonNull SupplyType getSupplyType() {
        return SupplyType.IRON;
    }

    @Override
    public @NonNull Supply respawn() {
        return new IronSupply(getWorld(), getGridX(), getGridY(), getPositionX(), getPositionY(), false);
    }

    @Override
    public float getSpawnTime() {
        return 6.0f;
    }

    @Override
    public Color.@Nullable Linear getSpawnColorTint() {
        return spawnColorTint;
    }

    @Override
    public Color.@Nullable Linear getCrackDecalColor() {
        return crackDecalColor;
    }

    @Override
    public float getCrackDecalOpacity() {
        return crackDecalOpacity;
    }

    @Override
    public float getCrackDecalDiameter() {
        return crackDecalDiameter;
    }

    @Override
    public float getCrackDecalPattern() {
        return crackDecalPattern;
    }

    @Override
    public float getOffsetZ() {
        float slope = getSlopeOffset();
        if (isSpawning()) {
            float fallProgress = Math.min(1.0f, getSpawnProgress() / FALL_DURATION_RATIO);
            return (1.0f - fallProgress) * SPAWN_OFFSET_Z + slope;
        }
        return slope;
    }

    @Override
    public void animateSpawn(float t, float progress) {
        super.animateSpawn(t, progress);
        if (progress < FALL_DURATION_RATIO) {
            // falling
            spawnColorTint = COLOR_FALLING;

            Vector3f pos = new Vector3f(getPositionX(), getPositionY(), getPositionZ());
            if (trailEmitter == null) {
                // Legacy baseline physical parameters (rate 400, spreads)
                RandomVelocityEmitter emitter = new RandomVelocityEmitter(
                        getWorld(), pos, 0.0f, 0.0f,
                        getSize() * 0.8f, 0.5f, 0.1f, 0.05f,
                        -1, 400f,
                        new Vector3f(0f, 0f, -120.0f), new Vector3f(0f, 0f, 150.0f),
                        new Color.Linear(0.02f, 0.75f), new Color.LinearDelta(0f, 0f, 0f, -0.75f),
                        new Vector3f(1.0f, 1.0f, 2.0f), new Vector3f(3.5f, 3.5f, 7.0f), // Vertically Stretched
                        0.8f, 0.1f,
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        getWorld().getRacesResources().getSmokeTextures()
                );
                // Specifically requested sooty/black anchor
                emitter.setSpectrumRange(0.2f, 0.2f);
                emitter.setJitterIntensity(0.005f);
                trailEmitter = new PointEmitterModel(getWorld(), emitter);
            } else {
                trailEmitter.setPosition(getPositionX(), getPositionY(), getPositionZ());
            }
        } else {
            if (trailEmitter != null) {
                trailEmitter.getEmitter().done();
                trailEmitter = null;
            }

            if (!landed) {
                landed = true;
                setShowShadow(true);

                Vector3f landingPos = new Vector3f(getPositionX(), getPositionY(), getPositionZ() + 0.15f);
                new SonicBlastEffect(getWorld(), landingPos, 3.5f, 0.4f, new Color.Linear(0.2f, 0.25f, 0.3f, 1.0f));

                // Fewer particles (24), more vertical stretch (1.5 radius, 15.0 growth)
                RingEmitter puff = new RingEmitter(
                        getWorld(), landingPos, 0.0f,
                        0.2f, 0.1f,
                        24, 2000f,
                        new Vector3f(0f, 0f, 15.0f),
                        new Vector3f(0f, 0f, 16.0f),
                        new Color.Linear(0.05f, 0.75f), Color.LinearDelta.ZERO.alpha(-0.2f),
                        new Vector3f(0.8f, 0.8f, 1.5f), // Stretched Radius Z=1.5
                        new Vector3f(8.5f, 8.5f, 15.0f), // Stretched Growth Z=15.0
                        0.5f, 0.1f,
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        getWorld().getRacesResources().getSmokeTextures()
                );

                puff.setSpectrumRange(0.0f, 0.6f);
                puff.setBaseColor(Landscape.getDustColor(getWorld().getTerrainType()));
                puff.setTransition(0.1f, 0.1f, 0.0f, 0.5f);
                puff.setJitterIntensity(0.01f);

                new PointEmitterModel(getWorld(), puff);

                getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                        new AudioParameters(AudioAssets.SFX_LURBLAST, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.8f
                        ));
            }

            // Cracks pulse logic (orange fading to black over 0.5s)
            float crackTime = 0.5f / 3.0f; // 0.5s duration
            if (progress >= FALL_DURATION_RATIO && progress < FALL_DURATION_RATIO + crackTime) {
                float crackProgress = (progress - FALL_DURATION_RATIO) / crackTime;
                crackDecalOpacity = 1.0f - crackProgress;
                crackDecalDiameter = getSize() * 2.0f;
                crackDecalPattern = 10.5f;
                float colorVal = 1.0f - crackProgress;
                crackDecalColor = new Color.Linear(colorVal, colorVal, colorVal, 1.0f);
            } else {
                crackDecalOpacity = 0.0f;
                crackDecalColor = null;
            }

            float coolProgress = (progress - 0.4f) / 0.6f;
            if (coolingEmitter == null && !cooling) {
                Vector3f pos = new Vector3f(getPositionX(), getPositionY(), getPositionZ());
                // Legacy baseline physical parameters
                RandomVelocityEmitter emitter = new RandomVelocityEmitter(
                        getWorld(), pos, 0.0f, 0.0f,
                        getSize() * 0.5f, 0.1f, 0.1f, 0.05f,
                        -1, 150f,
                        new Vector3f(0f, 0f, 1.5f), new Vector3f(0f, 0f, -0.3f),
                        new Color.Linear(0.1f, 0.1f, 0.1f, 0.5f), new Color.LinearDelta(0f, 0f, 0f, -0.6f),
                        new Vector3f(0.8f, 0.8f, 0.8f), new Vector3f(2.5f, 2.5f, 2.5f),
                        0.8f, 0.1f,
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        getWorld().getRacesResources().getSmokeTextures()
                );
                coolingEmitter = new PointEmitterModel(getWorld(), emitter);
            }

            if (coolProgress >= 0.0f) {
                float spawnRate = 150.0f * (1.0f - coolProgress);
                if (coolingEmitter != null) {
                    coolingEmitter.getEmitter().setParticlesPerSecond(spawnRate);
                }
                if (!cooling) {
                    cooling = true;
                    var sizzle = getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                            new AudioParameters(AudioAssets.SFX_GAS, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.65f
                            ));
                    sizzle.stop(10f);
                }
            }

            if (coolProgress < 0.3f) {
                float factor = coolProgress / 0.3f;
                spawnColorTint = new Color.Linear(
                        2.0f - 0.5f * factor,
                        0.8f - 0.8f * factor,
                        0.0f,
                        1.0f
                );
            } else if (coolProgress < 0.7f) {
                float factor = (coolProgress - 0.3f) / 0.4f;
                spawnColorTint = new Color.Linear(
                        1.5f - 1.35f * factor,
                        0.15f * factor,
                        0.2f * factor,
                        1.0f
                );
            } else {
                float factor = (coolProgress - 0.7f) / 0.3f;
                spawnColorTint = new Color.Linear(
                        0.15f + 0.85f * factor,
                        0.15f + 0.85f * factor,
                        0.2f + 0.8f * factor,
                        1.0f
                );
            }
        }
        reinsert();
    }

    @Override
    public void spawnComplete() {
        super.spawnComplete();
        setShowShadow(true);

        spawnColorTint = null;
        crackDecalColor = null;
        crackDecalOpacity = 0.0f;
        crackDecalDiameter = 0.0f;
        crackDecalPattern = 0.0f;
        landed = false;
        if (trailEmitter != null) {
            trailEmitter.getEmitter().done();
            trailEmitter = null;
        }
        if (coolingEmitter != null) {
            coolingEmitter.getEmitter().done();
            coolingEmitter = null;
        }
        cooling = false;
        reinsert();
    }
}
