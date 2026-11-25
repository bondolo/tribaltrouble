package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.effects.particle.RandomVelocityEmitter;
import com.oddlabs.tt.effects.render.EmitterAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.Stun;
import com.oddlabs.util.Color;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/// {@link VisualModel} implementation for the stun spell managing musical note particle emitters and audio.
final class StunVisualModel extends AbstractVisualModel implements EmitterAccessory {
  private final Stun stun;
  private final RandomVelocityEmitter emitter;
  private final AudioPlayer sound;

  private float age = 0f;

  StunVisualModel(Stun stun, AudioImplementation audio, TextureKey[] noteTextures) {
    super(stun);
    this.stun = stun;
    World world = stun.getWorld();
    Vector3f pos = new Vector3f(stun.getPositionX(), stun.getPositionY(), stun.getPositionZ());

    float alpha = 12f;
    float energy = 4f;
    this.emitter =
        new RandomVelocityEmitter(
            world,
            pos,
            0f,
            0f,
            .001f,
            .001f,
            .5f,
            (float) Math.PI,
            -1,
            35f,
            new Vector3f(0f, 0f, 6f),
            new Vector3f(0f, 0f, -2f),
            new Color.Linear(1f, 1f, 1f, alpha),
            new Color.LinearDelta(0f, 0f, 0f, -alpha / energy),
            new Vector3f(.4f, .4f, .4f),
            new Vector3f(.025f, .025f, .025f),
            energy,
            1f,
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA,
            noteTextures);

    this.sound =
        audio.newAudio(
            stun.getPositionX(),
            stun.getPositionY(),
            stun.getPositionZ(),
            AudioRegistry.STUN_LUR[ThreadLocalRandom.current().nextInt(AudioRegistry.STUN_LUR.length)]);
  }

  @Override
  public void animate(float dt) {
    if (!stun.isDead()) {
      emitter.getPosition().set(stun.getPositionX(), stun.getPositionY(), stun.getPositionZ());
    }

    age += dt;
    if (age > 1.5f) {
      emitter.done();
    }

    emitter.animate(dt);
  }

  @Override
  public Emitter<?> getEmitter() {
    return emitter;
  }

  @Override
  protected boolean isSelfExpired() {
    return stun.isDead() && emitter.isFinished();
  }

  @Override
  public boolean isVisible(Model parent, CameraState camera) {
    return !stun.isDead() || !emitter.isFinished();
  }

  @Override
  public void close() {
    super.close();
    sound.stop(10.0f);
  }
}
