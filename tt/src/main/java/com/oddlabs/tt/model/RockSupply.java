package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.landscape.World;
import java.util.concurrent.ThreadLocalRandom;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.render.LandscapeResources;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * A rock boulder supply that erupts from the ground.
 */
public final class RockSupply extends SupplyModel {
    private static final float SPAWN_OFFSET_Z = -2f;
    private static final int INITIAL_SUPPLIES = 10;

    private Color.@Nullable Linear spawnColorTint = null;
    private Color.@Nullable Linear crackDecalColor = null;
    private float crackDecalOpacity = 0.0f;
    private float crackDecalDiameter = 0.0f;
    private float crackDecalPattern = 0.0f;
    private @Nullable PointEmitterModel smokeEmitter = null;
    private boolean soundPlayed = false;

    public RockSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        var rotation = ThreadLocalRandom.current().nextFloat((float) -Math.PI, (float) Math.PI);
        var fragmentIndex = ThreadLocalRandom.current().nextInt(LandscapeResources.SUPPLY_FRAGMENT_COUNT);
        super(world, 2f, grid_x, grid_y, x, y, SPAWN_OFFSET_Z, rotation, INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getRockBounds(fragmentIndex));
    }

    @Override
    public @NonNull SupplyType getSupplyType() {
        return SupplyType.ROCK;
    }

    @Override
    public @NonNull Supply respawn() {
        return new RockSupply(getWorld(), getGridX(), getGridY(), getPositionX(), getPositionY(), false);
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

    private void ensureSmokeEmitter() {
        if (smokeEmitter == null) {
            Vector3f pos = new Vector3f(getPositionX(), getPositionY(), getPositionZ());
            RandomVelocityEmitter emitter = new RandomVelocityEmitter(
                    getWorld(), pos, 0.0f, 0.0f,
                    getSize() * 0.4f, 0.1f, 0.2f, 0.1f,
                    -1, 15f,
                    new Vector3f(0f, 0f, 2.0f), new Vector3f(0f, 0f, -0.5f),
                    new Color.Linear(0.35f, 0.35f, 0.35f, 0.6f), new Color.LinearDelta(0f, 0f, 0f, -0.6f),
                    new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(1.0f, 1.0f, 1.0f),
                    1.0f, 0.2f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    getWorld().getRacesResources().getSmokeTextures()
            );
            smokeEmitter = new PointEmitterModel(getWorld(), emitter);
        }
    }

    @Override
    public void animateSpawn(float t, float progress) {
        if (progress < 0.3f) {
            float progressRatio = progress / 0.3f;
            crackDecalOpacity = progressRatio;
            crackDecalDiameter = getSize() * 2.0f;
            crackDecalPattern = 10.0f + 0.5f * progressRatio;
            crackDecalColor = Color.Linear.WHITE;
            spawnColorTint = null;
            if (!soundPlayed) {
                soundPlayed = true;
                getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                        new AudioParameters(AudioAssets.SFX_RUMBLE, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION));
            }
            ensureSmokeEmitter();
        } else if (progress < 0.7f) {
            crackDecalOpacity = 1.0f;
            crackDecalDiameter = getSize() * 2.0f;
            crackDecalPattern = 10.5f;
            crackDecalColor = Color.Linear.WHITE;
            float riseProgress = (progress - 0.3f) / 0.4f;
            setOffsetZ(SPAWN_OFFSET_Z * (1.0f - riseProgress));
            spawnColorTint = new Color.Linear(2.0f, 0.8f, 0.0f, 1.0f);
            ensureSmokeEmitter();
        } else {
            float coolProgress = (progress - 0.7f) / 0.3f;
            setOffsetZ(0.0f);
            setShowShadow(true);
            crackDecalOpacity = 1.0f - coolProgress;
            crackDecalDiameter = getSize() * 2.0f;
            crackDecalPattern = 10.5f;
            crackDecalColor = new Color.Linear(1.0f - coolProgress, 1.0f);

            if (smokeEmitter != null) {
                smokeEmitter.getEmitter().done();
                smokeEmitter = null;
            }

            if (coolProgress < 0.5f) {
                float factor = coolProgress / 0.5f;
                spawnColorTint = new Color.Linear(
                        2.0f - 0.8f * factor,
                        0.8f - 0.3f * factor,
                        0.3f * factor,
                        1.0f
                );
            } else {
                float factor = (coolProgress - 0.5f) / 0.5f;
                spawnColorTint = new Color.Linear(
                        1.2f - 0.2f * factor,
                        0.5f + 0.5f * factor,
                        0.3f + 0.7f * factor,
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
        soundPlayed = false;
        if (smokeEmitter != null) {
            smokeEmitter.getEmitter().done();
            smokeEmitter = null;
        }
        reinsert();
    }
}
