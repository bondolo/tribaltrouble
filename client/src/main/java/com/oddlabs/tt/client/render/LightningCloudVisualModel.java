package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.effects.particle.CloudFunction;
import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.effects.particle.Lightning;
import com.oddlabs.tt.effects.particle.ParametricEmitter;
import com.oddlabs.tt.effects.render.EmitterAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.LightningAccessory;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.LightningCloud;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * {@link VisualModel} implementation for lightning clouds managing cloud particle emitters and lightning strikes.
 */
public final class LightningCloudVisualModel extends AbstractVisualModel implements EmitterAccessory,
        LightningAccessory {
    private static final float BRIGHTNESS = Color.toLinear(.2f);
    private static final Color.LinearDelta BRIGHTNESS_DELTA = new Color.LinearDelta(BRIGHTNESS, 0);
    private static final float LIGHTNING_TIME = .1f;
    private static final Color.LinearDelta DELTA_COLOR = Color.LinearDelta.ZERO.alpha(-1f / LIGHTNING_TIME);

    private static final float CLOUD_RADIUS_XY = 2.5f;
    private static final float CLOUD_RADIUS_Z = 0.7f;
    private static final float PARTICLE_RADIUS_XY = 2.0f;
    private static final float PARTICLE_RADIUS_Z = 0.7f;
    private static final float LIGHTING_INTENSITY = 0.10f;

    private final LightningCloud cloud;
    private final ParametricEmitter emitter;
    private final AudioImplementation audio;
    private @Nullable AudioPlayer cloudSound;

    private final Deque<Lightning> activeLightnings = new ArrayDeque<>();

    private float lightningTimer = 0f;
    private boolean lighted = false;
    private boolean firstRun = true;
    private float strikeAudioCooldown = 0f;

    public LightningCloudVisualModel(LightningCloud cloud, AudioImplementation audio) {
        super(cloud);
        this.cloud = cloud;
        this.audio = audio;
        World world = cloud.getWorld();
        Vector3f pos = new Vector3f(cloud.getPositionX(), cloud.getPositionY(), cloud.getPositionZ());

        float energy = cloud.getSecondsToLive() + cloud.getSecondsToInit();
        var function = new CloudFunction(CLOUD_RADIUS_XY, CLOUD_RADIUS_Z);
        this.emitter = new ParametricEmitter(world, function, pos,
                0f, cloud.getCloudOffsetZ(), .5f, .5f, .2f,
                45, 150f,
                Color.Linear.WHITE.alpha(0.8f), Color.LinearDelta.ZERO,
                new Vector3f(PARTICLE_RADIUS_XY, PARTICLE_RADIUS_XY, PARTICLE_RADIUS_Z), new Vector3f(0f, 0f, 0f),
                energy,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                AssetRegistry.getInstance().getSmokeTextures());

        this.emitter.setBaseColor(new Color.Standard(.3f, 1f).linear());
        this.emitter.setColorSpectrum((spectrum, baseColor) -> baseColor);
        this.emitter.setFogEnabled(false);
        this.emitter.setJitterIntensity(0.05f);
        this.emitter.setRandomizeScale(true);

        float maxLocalZ = CLOUD_RADIUS_Z * CloudFunction.TOP_PUFFINESS_PEAK;
        this.emitter.setHeightLighting(LIGHTING_INTENSITY, maxLocalZ);
    }

    @Override
    public void triggerStrike(float tx, float ty, float tz) {
        if (lightningTimer <= 0f) {
            emitter.adjustColor(BRIGHTNESS_DELTA);
            lightningTimer = LIGHTNING_TIME;
            lighted = true;

            if (strikeAudioCooldown <= 0f) {
                var params = new AudioParameters(
                        AudioRegistry.SFX_FLASH, AudioRegistry.AUDIO_RANK_MAGIC,
                        AudioRegistry.AUDIO_DISTANCE_MAGIC, AudioRegistry.AUDIO_GAIN_LIGHTNING,
                        AudioRegistry.AUDIO_RADIUS_LIGHTNING);
                audio.newAudio(tx, ty, tz, params);
                strikeAudioCooldown = 0.8f;
            }
        }

        Vector3f cloudPos = new Vector3f(cloud.getPositionX(), cloud.getPositionY(), cloud.getPositionZ()
                + cloud.getCloudOffsetZ());
        Lightning lightning = new Lightning(cloudPos, new Vector3f(tx, ty, tz), .5f,
                15, Color.Linear.WHITE, DELTA_COLOR,
                AssetRegistry.getInstance().getLightningTexture(), LIGHTNING_TIME);
        activeLightnings.add(lightning);
    }

    @Override
    public void animate(float t) {
        for (var lightning : activeLightnings) {
            lightning.animate(t);
        }
        activeLightnings.removeIf(Lightning::isFinished);
        if (firstRun) {
            cloudSound = audio.newAudio(cloud.getPositionX(), cloud.getPositionY(), cloud
                    .getPositionZ(),
                    AudioRegistry.LIGHTNING_CLOUD);
            firstRun = false;
        }
        if (cloudSound != null) {
            cloudSound.setPosition(cloud.getPositionX(), cloud.getPositionY(), cloud.getPositionZ());
        }
        if (strikeAudioCooldown > 0f) {
            strikeAudioCooldown -= t;
        }

        // Update emitter position
        emitter.getPosition().set(cloud.getPositionX(), cloud.getPositionY(), cloud.getPositionZ());

        // Handle cloud fade out in the last 2 seconds
        float secondsToLive = cloud.getSecondsToLive();
        if (secondsToLive <= 2.0f) {
            emitter.adjustColor(new Color.LinearDelta(0f, -0.8f * t / 2.0f));
        }

        // Handle lightning strike cloud flash dimming
        if (lighted) {
            lightningTimer -= t;
            if (lightningTimer <= 0) {
                emitter.adjustColor(BRIGHTNESS_DELTA.negate());
                lighted = false;
            }
        }

        emitter.animate(t);
    }

    public Deque<Lightning> getActiveLightnings() {
        return activeLightnings;
    }

    @Override
    public Emitter<?> getEmitter() {
        return emitter;
    }

    @Override
    protected boolean isSelfExpired() {
        return cloud.isDead() && activeLightnings.isEmpty();
    }

    @Override
    public boolean isVisible(Model parent, CameraState camera) {
        return !cloud.isDead() || !activeLightnings.isEmpty();
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
        if (cloudSound != null) {
            cloudSound.stop(15.0f);
            cloudSound = null;
        }
    }
}
