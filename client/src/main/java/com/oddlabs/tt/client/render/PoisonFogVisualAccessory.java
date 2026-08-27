package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.effects.particle.RandomVelocityEmitter;
import com.oddlabs.tt.effects.render.EmitterAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.PoisonFog;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side visual accessory for the poison fog magical effect.
 * Periodically spawns poison gas puffs inside the fog region on the client.
 */
public final class PoisonFogVisualAccessory implements EmitterAccessory {
    private static final int PARTICLES_PER_BURST = 4;
    private static final float SECONDS_BETWEEN_BURSTS = .15f;
    private static final float BURST_RADIUS = 2f;
    private static final float GAUSSIAN_LIMIT = 2.5f;
    private static final int MIN_BURSTS_PER_SOUND = 2;

    private final PoisonFog poisonFog;
    private final AudioImplementation audio;
    private final AudioPlayer bubblingSound;
    private final Deque<Emitter<?>> burstEmitters = new ArrayDeque<>();

    private float time = 0f;
    private int bursts = 0;
    private int nextSound = 1;

    public PoisonFogVisualAccessory(PoisonFog poisonFog, AudioImplementation audio) {
        this.poisonFog = poisonFog;
        this.audio = audio;
        World world = poisonFog.getWorld();
        this.bubblingSound = audio.newAudio(poisonFog.getPositionX(), poisonFog.getPositionY(),
                world.getHeightMap().getNearestHeight(poisonFog.getPositionX(), poisonFog.getPositionY()),
                AudioAssets.BUBBLING);
    }

    @Override
    public void animate(float t) {
        time += t;
        World world = poisonFog.getWorld();
        Random random = world.getRandom();

        if (bursts * SECONDS_BETWEEN_BURSTS < time) {
            float gaussian = (float) (GAUSSIAN_LIMIT - Math.abs(Math.clamp(random.nextGaussian(),
                    -GAUSSIAN_LIMIT, GAUSSIAN_LIMIT))) / GAUSSIAN_LIMIT;
            float r = gaussian * (poisonFog.getHitRadius() - BURST_RADIUS - 5f);
            float a = random.nextFloat(0f, (float) Math.PI * 2);
            float x = poisonFog.getPositionX() + (float) Math.cos(a) * r;
            float y = poisonFog.getPositionY() + (float) Math.sin(a) * r;
            float z = world.getHeightMap().getNearestHeight(x, y);
            float alpha = 8f;
            float energy = 2f;

            RandomVelocityEmitter emitter = new RandomVelocityEmitter(world, new Vector3f(x, y, z), poisonFog
                    .getCloudOffsetZ(),
                    random.nextFloat(0f, (float) Math.PI * 2),
                    BURST_RADIUS, 0f, 0f, 0f,
                    PARTICLES_PER_BURST, PARTICLES_PER_BURST,
                    new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f),
                    new Color.Linear(1f, 1f, 1f, alpha), new Color.LinearDelta(0f, 0f, 0f, -alpha / energy),
                    new Vector3f(0f, 0f, .25f), new Vector3f(3.5f, 3.5f, 0f), energy, 1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    AssetRegistry.getInstance().getPoisonTextures());
            burstEmitters.add(emitter);

            if (bursts % nextSound == 0) {
                nextSound = MIN_BURSTS_PER_SOUND + ThreadLocalRandom.current().nextInt(5);
                audio.newAudio(x, y, z, AudioAssets.POISON_GAS);
            }
            bursts++;
        }

        for (var e : burstEmitters) {
            e.animate(t);
        }
        burstEmitters.removeIf(Emitter::isFinished);
    }

    @Override
    public @Nullable Emitter<?> getEmitter() {
        return null;
    }

    @Override
    public void addEmitters(java.util.Collection<Emitter<?>> queue) {
        queue.addAll(burstEmitters);
    }

    @Override
    public boolean isExpired() {
        return poisonFog.isDead() && burstEmitters.isEmpty();
    }

    @Override
    public boolean isVisible(Model parent, CameraState camera) {
        return !poisonFog.isDead() || !burstEmitters.isEmpty();
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
        bubblingSound.stop(15.0f);
    }
}
