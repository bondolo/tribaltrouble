package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.landscape.World;
import java.util.concurrent.ThreadLocalRandom;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.particle.RingEmitter;
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
    private static final float SPAWN_OFFSET_Z = 200.0f;
    private static final float FALL_DURATION_RATIO = 0.12f;

    private static final Color.Linear COLOR_FALLING = new Color.Linear(2.0f, 0.5f, 0.05f, 1.0f);
    private static final Color.Linear COLOR_LANDING = new Color.Linear(2.0f, 1.0f, 0.2f, 1.0f); // Overdriven
    private static final Color.Linear COLOR_HOT = new Color.Linear(2.0f, 0.2f, 0.1f, 1.0f); // Overdriven
    private static final Color.Linear COLOR_WHITE_HOT = new Color.Linear(2.0f, 2.0f, 2.0f, 1.0f); // Overdriven
    private static final Color.Linear COLOR_COOLING = new Color.Standard(0xFF_6B_6B_7A).linear();
    private static final Color.Linear COLOR_DECAL_COOLED = Color.Linear.BLACK;
    private static final float SMOKE_PARTICLES_PER_SECOND = 30.0f; // Lower density

    private Color.@Nullable Linear spawnColorTint = COLOR_FALLING;
    private Color.@Nullable Linear crackDecalColor = null;
    private float crackDecalOpacity = 0.0f;
    private float crackDecalDiameter = 0.0f;
    private float crackDecalPattern = 0.0f;
    private @Nullable PointEmitterModel trailEmitter = null;
    private @Nullable PointEmitterModel coolingEmitter = null;
    private final int fragmentIndex;
    private boolean landed = false;
    private boolean cooling = false;
    private boolean airBurstPlayed = false;
    private boolean useRockTexture = true;

    public IronSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        this(world, grid_x, grid_y, x, y, increase, ThreadLocalRandom.current().nextInt(LandscapeResources.SUPPLY_FRAGMENT_COUNT));
    }

    private IronSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase, int fragmentIndex) {
        super(world, 2f, grid_x, grid_y, x, y, SPAWN_OFFSET_Z, ThreadLocalRandom.current().nextFloat((float) -Math.PI, (float) Math.PI), INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getIronBounds(fragmentIndex));
        this.fragmentIndex = fragmentIndex;
    }

    @Override
    public @NonNull BoundsProvider getBoundsProvider() {
        // Use rock texture while hot to show tinting better
        if (useRockTexture) {
            return getWorld().getLandscapeResources().getRockBounds(fragmentIndex);
        }
        return super.getBoundsProvider();
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
        return 4.5f;
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

            if (!airBurstPlayed) {
                airBurstPlayed = true;
                getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                        new AudioParameters(AudioAssets.SFX_LURBLAST, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.8f
                        ));
            }

            float fallProgress = progress / FALL_DURATION_RATIO;
            var emitterModel = ensureTrailEmitter();
            var emitter = (RandomVelocityEmitter) emitterModel.getEmitter();

            // Dynamic Parameters:
            // PPS: Scaling 800 to 2400 (Tripled at landing)
            float pps = 800f + 1600f * fallProgress;
            emitter.setParticlesPerSecond(pps);

            // Size/Stretch: 
            // Start: 1.6 X/Z, 2.0 Y (Double size and moderate stretch)
            // Landing: 0.8 X/Z, 0.75 Y (Baseline size, halved stretch)
            float radiusXZ = 1.6f - 0.8f * fallProgress;
            float radiusY = 2.0f - 1.25f * fallProgress;
            emitter.setParticleRadius(radiusXZ, radiusY, radiusXZ);

            // Growth scales proportionally
            float growthXZ = 2.4f - 1.2f * fallProgress;
            float growthY = 3.0f - 2.25f * fallProgress;
            emitter.setGrowthRate(growthXZ, growthY, growthXZ);

            emitterModel.setPosition(getPositionX(), getPositionY(), getPositionZ());
        } else {
            if (trailEmitter != null) {
                trailEmitter.getEmitter().done();
                trailEmitter = null;
            }

            if (!landed) {
                landed = true;
                setShowShadow(true);

                Vector3f landingPos = new Vector3f(getPositionX(), getPositionY(), getPositionZ() + 0.15f);

                // Bright impact flash (additive)
                RingEmitter flash = new RingEmitter(
                        getWorld(), landingPos, 0.0f,
                        0.0f, 0.0f,
                        1, 1000f,
                        new Vector3f(0f, 0f, 0.0f),
                        new Vector3f(0f, 0f, 0.0f),
                        COLOR_WHITE_HOT, Color.LinearDelta.ZERO.alpha(-10.0f), // Very fast fade
                        new Vector3f(10.0f, 10.0f, 10.0f),
                        new Vector3f(0.0f, 0.0f, 0.0f),
                        0.1f, 0.0f,
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                        getWorld().getRacesResources().getSmokeTextures()
                );
                new PointEmitterModel(getWorld(), flash);

                // Primary energetic dust puff (high speed, explosive)
                RingEmitter puff = new RingEmitter(
                        getWorld(), landingPos, 0.0f,
                        0.5f, 0.0f, // Lowered emitter height
                        48, 10f,
                        new Vector3f(0f, 0f, 40.0f), // Balanced expansion
                        new Vector3f(0f, 0f, 0.0f), // No lift
                        Color.Linear.WHITE.alpha(0.30f), Color.LinearDelta.ZERO,
                        new Vector3f(1.0f, 1.0f, 0.0f), // Flat (radius.z)
                        new Vector3f(15.0f, 15.0f, 0.0f), // Flat (growth.z)
                        0.6f, 0.1f, // Shorter energy
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        getWorld().getRacesResources().getSmokeTextures()
                );

                puff.setSpectrumRange(0.0f, 0.9f);
                puff.setBaseColor(Landscape.getDustColor(getWorld().getTerrainType()));
                puff.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum * 0.5f));
                puff.setTransition(0.1f, 0.1f, 0.0f, 0.5f);
                puff.setJitterIntensity(0.15f);

                new PointEmitterModel(getWorld(), puff);

                // Secondary lingering debris cloud (ground sweeping)
                RingEmitter debris = new RingEmitter(
                        getWorld(), landingPos, 0.0f,
                        0.5f, 0.0f, // Halved emitter radius
                        64, 10f,
                        new Vector3f(0f, 0f, 6.0f), // Halved radial expansion speed
                        new Vector3f(0f, 0f, 0.0f), // No lift
                        Color.Linear.WHITE.alpha(0.15f), Color.LinearDelta.ZERO,
                        new Vector3f(1.0f, 0.2f, 0.0f), // Halved particle radius
                        new Vector3f(4.0f, 0.25f, 0.0f), // Halved growth rate
                        1.0f, 0.45f, // Halved lifetime/energy (adjusts fadeout)
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        getWorld().getRacesResources().getSmokeTextures()
                );
                debris.setBaseColor(Landscape.getDustColor(getWorld().getTerrainType()));
                debris.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum * 0.4f));
                debris.setSpectrumRange(0.1f, 0.9f);
                debris.setJitterIntensity(0.20f);
                new PointEmitterModel(getWorld(), debris);
            }

            // Cracks pulse logic (fading out smoothly)
            float crackDuration = 0.6f; 
            if (progress >= FALL_DURATION_RATIO && progress < FALL_DURATION_RATIO + crackDuration) {
                float crackProgress = (progress - FALL_DURATION_RATIO) / crackDuration;
                crackDecalOpacity = 1.0f - crackProgress;
                crackDecalDiameter = getSize() * 2.5f; // Matched RockSupply
                crackDecalPattern = 10.5f;
                // Fade color from White to Cooled (Black)
                crackDecalColor = Color.Linear.WHITE.lerp(COLOR_DECAL_COOLED, crackProgress);
            } else {
                crackDecalOpacity = 0.0f;
                crackDecalColor = null;
            }

            float coolProgress = Math.min(1.0f, (progress - FALL_DURATION_RATIO) / (0.85f - FALL_DURATION_RATIO));

            if (!cooling) {
                cooling = true;
                ensureCoolingEmitter();
                var sizzle = getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                        new AudioParameters(AudioAssets.SFX_GAS, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.65f
                        ));
                sizzle.stop(1.3f);
            }
            ensureCoolingEmitter().getEmitter().setParticlesPerSecond(SMOKE_PARTICLES_PER_SECOND * (1.0f - coolProgress));

            if (coolProgress < 0.3f) {
                float factor = coolProgress / 0.3f;
                spawnColorTint = COLOR_FALLING.lerp(COLOR_LANDING, factor);
            } else if (coolProgress < 0.6f) {
                float factor = (coolProgress - 0.3f) / 0.3f;
                spawnColorTint = COLOR_LANDING.lerp(COLOR_HOT, factor);
            } else if (coolProgress < 0.8f) {
                float factor = (coolProgress - 0.6f) / 0.2f;
                spawnColorTint = COLOR_HOT.lerp(COLOR_COOLING, factor);
            } else if (coolProgress < 0.9f) {
                float factor = (coolProgress - 0.8f) / 0.1f;
                spawnColorTint = COLOR_COOLING.lerp(COLOR_COOLING.mul(0.35f), factor);
            } else if (coolProgress < 1.0f) {
                useRockTexture = false; // Transition to iron texture as we hit cool grey
                float factor = (coolProgress - 0.9f) / 0.1f;
                Color.Linear ironStartTint = COLOR_COOLING.mul(0.9f);
                spawnColorTint = ironStartTint.lerp(Color.Linear.WHITE, factor);
            } else {
                spawnColorTint = null;
            }
        }
    }

    private PointEmitterModel ensureTrailEmitter() {
        if (trailEmitter == null) {
            // Offset UP more to ensure trail starts behind meteor center
            Vector3f pos = new Vector3f(getPositionX(), getPositionY(), getPositionZ() + 10.0f);
            RandomVelocityEmitter emitter = new RandomVelocityEmitter(
                    getWorld(), pos, 0.0f, 0.0f, // world, position, offset_z, uv_angle
                    0.02f, 5.0f, // Narrow vertical column
                    0.1f, 0.02f, // Reduced drift to keep column straight
                    -1, 800f, // Initial PPS (will be updated dynamically)
                    new Vector3f(0f, 0f, 10.0f), new Vector3f(0f, 0f, 5.0f), 
                    new Color.Linear(0.08f, 0.6f), new Color.LinearDelta(0f, -0.6f),
                    new Vector3f(1.6f, 2.0f, 1.6f), new Vector3f(2.4f, 3.0f, 2.4f), 
                    1.2f, 0.1f, 
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    getWorld().getRacesResources().getSmokeTextures(),
                    null, getWorld().getRacesResources().getSmokeTextures().length,
                    true, true 
            );
            // Specifically requested sooty/black anchor
            emitter.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum));
            emitter.setSpectrumRange(0.2f, 0.2f);
            emitter.setJitterIntensity(0.5f); // High jitter for distinct particles
            trailEmitter = new PointEmitterModel(getWorld(), emitter);
        }
        return trailEmitter;
    }

    private PointEmitterModel ensureCoolingEmitter() {
        if (coolingEmitter == null) {
            Vector3f pos = new Vector3f(getPositionX(), getPositionY(), getPositionZ());
            RandomVelocityEmitter emitter = new RandomVelocityEmitter(
                    getWorld(), pos, 0.0f, 0.0f,
                    getSize() * 0.5f, 0.1f, 0.2f, 0.1f,
                    -1, SMOKE_PARTICLES_PER_SECOND,
                    new Vector3f(0f, 0f, 1.5f), new Vector3f(0f, 0f, -0.3f),
                    new Color.Linear(0.15f, 0.5f), new Color.LinearDelta(0f, -0.4f),
                    new Vector3f(0.8f, 0.8f, 0.8f), new Vector3f(2.5f, 2.5f, 2.5f),
                    1.2f, 0.1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    getWorld().getRacesResources().getSmokeTextures()
            );
            // Spectrum 0.2 (dark grey, visible) → 0.8 (light grey, faint)
            emitter.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum));
            emitter.setSpectrumRange(0.2f, 0.8f);
            emitter.setJitterIntensity(0.02f);
            emitter.setTransition(0f, 3.0f, 0.8f, 0f);
            coolingEmitter = new PointEmitterModel(getWorld(), emitter);
        }
        return coolingEmitter;
    }

    @Override
    public void spawnComplete() {
        super.spawnComplete();

        spawnColorTint = null;
        crackDecalColor = null;
        crackDecalOpacity = 0.0f;
        crackDecalDiameter = 0.0f;
        crackDecalPattern = 0.0f;
        landed = false;
        airBurstPlayed = false;
        useRockTexture = false;
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
