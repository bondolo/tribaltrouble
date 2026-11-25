package com.oddlabs.tt.effects.particle;

import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.util.Color;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/// A specialized parametric emitter that maintains a balanced distribution of particles.
public final class BalancedParametricEmitter extends ParametricEmitter {
  private final int num_particles;
  private final float dist_u;
  private final float dist_v;
  private final float margin_u;
  private final float margin_v;

  public BalancedParametricEmitter(
      World world,
      ParametricFunction function,
      Vector3f position,
      float velocity_u,
      float velocity_v,
      float dist_u,
      float dist_v,
      int num_particles,
      float margin_u,
      float margin_v,
      Color.Linear color,
      Color.LinearDelta delta_color,
      Vector3fc particle_radius,
      Vector3fc growth_rate,
      float energy,
      int src_blend_func,
      int dst_blend_func,
      TextureKey[] textures) {
    super(
        world,
        function,
        position,
        0f,
        0f,
        velocity_u,
        velocity_v,
        0f,
        num_particles,
        Float.MAX_VALUE,
        color,
        delta_color,
        particle_radius,
        growth_rate,
        energy,
        src_blend_func,
        dst_blend_func,
        textures);
    this.num_particles = num_particles;
    this.dist_u = dist_u;
    this.dist_v = dist_v;
    this.margin_u = margin_u;
    this.margin_v = margin_v;
  }

  @Override
  protected int initParticle(
      ParametricFunction function,
      float velocity_u,
      float velocity_v,
      float colorR,
      float colorG,
      float colorB,
      float colorA,
      float deltaR,
      float deltaG,
      float deltaB,
      float deltaA,
      Vector3fc particle_radius,
      Vector3fc growth_rate,
      float energy) {

    for (int i = 0; i < num_particles; i++) {
      float u = dist_u * i / num_particles;
      float v = dist_v * i / num_particles;
      ParametricParticle particle = new ParametricParticle(function, u, v, 0f, 0f, 0f);
      Vector3f offset = randomOffset(margin_u, margin_v, 0f);
      particle.setVelocity(velocity_u + offset.x(), velocity_v + offset.y());
      particle.setColor(colorR, colorG, colorB, colorA);
      particle.setDeltaColor(deltaR, deltaG, deltaB, deltaA);
      particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
      particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
      particle.setEnergy(energy);
      particle.setType(ThreadLocalRandom.current().nextInt(getTypes()));
      particle.update(0);
      add(particle);
    }
    return num_particles;
  }
}
