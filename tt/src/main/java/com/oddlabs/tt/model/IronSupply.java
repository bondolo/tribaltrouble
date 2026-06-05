package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.particle.RingEmitter;
import com.oddlabs.tt.particle.SonicBlastEffect;
import com.oddlabs.tt.render.SpriteKey;
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

    private Color.@Nullable Linear spawnColorTint = null;
    private Color.@Nullable Linear crackDecalColor = null;
    private float crackDecalOpacity = 0.0f;
    private float crackDecalDiameter = 0.0f;
    private float crackDecalPattern = 0.0f;
    private float offsetZ = 0.0f;
    private @Nullable PointEmitterModel trailEmitter = null;
    private @Nullable PointEmitterModel coolingEmitter = null;
    private boolean landed = false;

    public IronSupply(@NonNull World world, @NonNull SpriteKey sprite_renderer, int grid_x, int grid_y,
            float x, float y, boolean increase) {
        var rotation = (float) (world.getRandom().nextDouble() * 2d * Math.PI);
        super(world, sprite_renderer, 2f, grid_x, grid_y, x, y, SPAWN_OFFSET_Z, rotation, INITIAL_SUPPLIES, increase);
    }

    @Override
    public @NonNull SpriteKey getStatusSprite(@NonNull RacesResources resources) {
        return resources.getIronStatusSprite();
    }

    @Override
    public @NonNull IronSupply respawn() {
        return new IronSupply(getWorld(), getSpriteRenderer(), getGridX(), getGridY(), getPositionX(),
                getPositionY(), false);
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
        return offsetZ + calculateSlopeOffset();
    }

    @Override
    public void animateSpawn(float t, float progress) {
        if (progress < FALL_DURATION_RATIO) {
            float fallProgress = progress / FALL_DURATION_RATIO;
            offsetZ = SPAWN_OFFSET_Z * (1.0f - fallProgress);
            spawnColorTint = new Color.Linear(2.0f, 0.8f, 0.0f, 1.0f);

            Vector3f pos = new Vector3f(getPositionX(), getPositionY(), getPositionZ() + offsetZ);
            if (trailEmitter == null) {
                RandomVelocityEmitter emitter = new RandomVelocityEmitter(
                        getWorld(), pos, 0.0f, 0.0f,
                        getSize() * 0.8f, 0.5f, 0.1f, 0.05f,
                        -1, 400f,
                        new Vector3f(0f, 0f, -120.0f), new Vector3f(0f, 0f, 150.0f),
                        new Color.Linear(0.06f, 0.06f, 0.06f, 0.6f), new Color.LinearDelta(0f, 0f, 0f, -0.75f),
                        new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(3.5f, 3.5f, 3.5f),
                        0.8f, 0.1f,
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        getWorld().getRacesResources().getSmokeTextures()
                );
                trailEmitter = new PointEmitterModel(getWorld(), emitter);
            } else {
                trailEmitter.setPositionZ(getPositionZ() + offsetZ);
            }
            setShowShadow(false);
        } else {
            offsetZ = 0.0f;

            if (trailEmitter != null) {
                trailEmitter.getEmitter().done();
                trailEmitter = null;
            }

            if (!landed) {
                landed = true;
                Vector3f landingPos = new Vector3f(getPositionX(), getPositionY(), getPositionZ() + 0.15f);
                new SonicBlastEffect(getWorld(), landingPos, 3.5f, 0.4f, new Color.Linear(0.2f, 0.25f, 0.3f, 1.0f));

                RingEmitter puff = new RingEmitter(
                        getWorld(), landingPos, 0.0f,
                        0.2f, 0.1f, // emitter_radius, emitter_height
                        48, 4000f,  // num_particles, particles_per_second
                        new Vector3f(0f, 0f, 15.0f), // velocity (Z component is horizontal expansion speed)
                        new Vector3f(0f, 0f, 16.0f),   // acceleration (positive Z rises upwards)
                        new Color.Linear(new Color.Standard(0x1A_FF_CC_99)), new Color.LinearDelta(0f, 0f, 0f, -0.2f),
                        new Vector3f(0.8f, 0.8f, 0.8f), new Vector3f(8.5f, 8.5f, 8.5f),
                        0.5f, 0.1f,
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        getWorld().getRacesResources().getSmokeTextures()
                );
                new PointEmitterModel(getWorld(), puff);

                getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                        new AudioParameters(AudioAssets.SFX_LURBLAST, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.8f
                        ));
            }
            setShowShadow(true);

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
            if (coolingEmitter == null) {
                Vector3f pos = new Vector3f(getPositionX(), getPositionY(), getPositionZ());
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
            } else if (coolProgress >= 0.0f) {
                float spawnRate = 150.0f * (1.0f - coolProgress);
                coolingEmitter.getEmitter().setParticlesPerSecond(spawnRate);
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
        spawnColorTint = null;
        crackDecalColor = null;
        crackDecalOpacity = 0.0f;
        crackDecalDiameter = 0.0f;
        crackDecalPattern = 0.0f;
        offsetZ = 0.0f;
        landed = false;
        setShowShadow(true);
        if (trailEmitter != null) {
            trailEmitter.getEmitter().done();
            trailEmitter = null;
        }
        if (coolingEmitter != null) {
            coolingEmitter.getEmitter().done();
            coolingEmitter = null;
        }
    }
}
