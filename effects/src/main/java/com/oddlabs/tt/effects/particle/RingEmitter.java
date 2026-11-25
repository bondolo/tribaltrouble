package com.oddlabs.tt.effects.particle;

import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.util.Color;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/// A linear emitter that spawns particles in a ring formation.
public final class RingEmitter extends LinearEmitter {
  private final int num_particles;

  public RingEmitter(
      World world,
      Vector3f position,
      float offset_z,
      float emitter_radius,
      float emitter_height,
      int num_particles,
      float particles_per_second,
      Vector3f velocity,
      Vector3f acceleration,
      Color.Linear color,
      Color.LinearDelta delta_color,
      Vector3f particle_radius,
      Vector3f growth_rate,
      float energy,
      float friction,
      int src_blend_func,
      int dst_blend_func,
      TextureKey[] textures) {
    super(
        world,
        position,
        offset_z,
        emitter_radius,
        emitter_height,
        num_particles,
        particles_per_second,
        velocity,
        acceleration,
        color,
        delta_color,
        particle_radius,
        growth_rate,
        energy,
        friction,
        src_blend_func,
        dst_blend_func,
        textures,
        null,
        textures.length);
    this.num_particles = num_particles;
  }

  @Override
  protected int initParticle(
      Vector3f position,
      Vector3fc velocity,
      Vector3fc acceleration,
      float templateColorR,
      float templateColorG,
      float templateColorB,
      float templateColorA,
      float deltaR,
      float deltaG,
      float deltaB,
      float deltaA,
      Vector3fc particle_radius,
      Vector3fc growth_rate,
      float energy) {
    float baseAngle = 2 * (float) Math.PI / num_particles;
    var clusterColor = getClusterColor();
    for (int i = 0; i < num_particles; i++) {
      LinearParticle particle = new LinearParticle();
      Vector3f pos = position;
      particle.setPos(pos.x(), pos.y(), pos.z());

      // Randomize radial velocity and energy for "blotchy" expansion
      float velocityMultiplier = ThreadLocalRandom.current().nextFloat(0.7f, 1.3f);
      float energyMultiplier = ThreadLocalRandom.current().nextFloat(0.4f, 1.6f);

      // Per-particle color (called per-particle for individual variation within the ring)
      computeNextParticleColor(clusterColor, this.color);
      float actualEnergy = energy * energyMultiplier;
      float baseFadeRate = actualEnergy > 0f ? -nextColorA / actualEnergy : 0f;
      float fadeMultiplier = ThreadLocalRandom.current().nextFloat(0.8f, 1.2f);
      float particleDeltaA = baseFadeRate * fadeMultiplier;

      // Jitter the angle to break up perfect rings
      float angle = baseAngle * i + ThreadLocalRandom.current().nextFloat(-0.1f, 0.1f);

      // in this special case velocity.getZ() is the actual velocity. not the velocity in the z direction
      particle.setVelocity(
          velocity.z() * velocityMultiplier * (float) Math.cos(angle),
          velocity.z() * velocityMultiplier * (float) Math.sin(angle),
          0);
      particle.setAcceleration(acceleration.x(), acceleration.y(), acceleration.z());
      particle.setColor(nextColorR, nextColorG, nextColorB, nextColorA);
      particle.setDeltaColor(deltaR, deltaG, deltaB, particleDeltaA);
      particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
      particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
      particle.setEnergy(energy * energyMultiplier);
      particle.setType(nextType());
      add(particle);
    }
    return num_particles;
  }
}
