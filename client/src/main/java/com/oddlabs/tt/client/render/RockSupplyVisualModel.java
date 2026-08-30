package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.effects.particle.RandomVelocityEmitter;
import com.oddlabs.tt.effects.render.EmitterAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Collection;

/**
 * {@link VisualModel} implementation for rock supplies managing eruption smoke particles and rumble audio.
 */
public final class RockSupplyVisualModel extends AbstractVisualModel implements EmitterAccessory {
    private final RockSupply rockSupply;
    private final AudioImplementation audio;
    private @Nullable RandomVelocityEmitter smokeEmitter = null;
    private boolean soundPlayed = false;

    public RockSupplyVisualModel(RockSupply rockSupply, AudioImplementation audio) {
        super(rockSupply);
        this.rockSupply = rockSupply;
        this.audio = audio;
    }

    @Override
    public void animate(float t) {
        if (SupplyVisualState.isSpawning(rockSupply)) {
            float progress = SupplyVisualState.getSpawnProgress(rockSupply);
            if (progress < 0.3f) {
                ensureSmokeEmitter().setTransition(0.0f, 1.8f, 0.2f, 0.3f);
                if (!soundPlayed) {
                    soundPlayed = true;
                    audio.newAudio(rockSupply.getPositionX(), rockSupply.getPositionY(),
                            rockSupply.getPositionZ(),
                            new AudioParameters(AudioAssets.SFX_RUMBLE, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION));
                }
            } else if (progress < 0.7f) {
                if (progress > 0.65f) {
                    ensureSmokeEmitter().setTransition(0.0f, 0.5f, 1.0f, 1.0f);
                }
            } else {
                cleanupEmitters();
            }

            if (smokeEmitter != null) {
                smokeEmitter.getPosition().set(rockSupply.getPositionX(), rockSupply.getPositionY(), rockSupply
                        .getPositionZ());
                smokeEmitter.animate(t);
            }
        } else {
            cleanupEmitters();
            soundPlayed = false;
        }
    }

    private void cleanupEmitters() {
        if (smokeEmitter != null) {
            smokeEmitter.done();
            smokeEmitter = null;
        }
    }

    private RandomVelocityEmitter ensureSmokeEmitter() {
        if (smokeEmitter == null) {
            World world = rockSupply.getWorld();
            Vector3f pos = new Vector3f(rockSupply.getPositionX(), rockSupply.getPositionY(), rockSupply
                    .getPositionZ());
            smokeEmitter = new RandomVelocityEmitter(
                    world, pos, 0.0f, 0.0f,
                    rockSupply.getSize() * 0.4f, 0.1f, 0.2f, 0.1f,
                    -1, 15.0f,
                    new Vector3f(0f, 0f, 2.0f), new Vector3f(0f, 0f, -0.5f),
                    new Color.Linear(0.1f, 0.75f), new Color.LinearDelta(0f, -0.6f),
                    new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(1.0f, 1.0f, 1.0f),
                    1.0f, 0.2f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    AssetRegistry.getInstance().getSmokeTextures()
            );
            smokeEmitter.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum));
            smokeEmitter.setSpectrumRange(0.2f, 1.0f);
        }
        return smokeEmitter;
    }

    @Override
    public void addEmitters(Collection<Emitter<?>> dest) {
        if (smokeEmitter != null) {
            dest.add(smokeEmitter);
        }
    }

    @Override
    protected boolean isSelfExpired() {
        return rockSupply.isDead();
    }

    @Override
    public boolean isVisible(Model parent, CameraState camera) {
        return !rockSupply.isDead();
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
