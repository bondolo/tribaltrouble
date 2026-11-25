package com.oddlabs.tt.effects.particle;

import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.util.Color;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/// A linear emitter that applies randomized initial velocities to its particles.
public final class RandomVelocityEmitter extends LinearEmitter {

  private final float uv_angle;
  private final float angle_bound;
  private final float angle_max_jump;
  private final Vector3f current_velocity;
  private final boolean randomizeRotation;
  private final boolean randomizeScale;

  private float x_angle = 0;
  private float y_angle = 0;

  public RandomVelocityEmitter(
      World world,
      Vector3f position,
      float offset_z,
      float uv_angle,
      float emitter_radius,
      float emitter_height,
      float angle_bound,
      float angle_max_jump,
      int num_particles,
      float particles_per_second,
      Vector3fc velocity,
      Vector3fc acceleration,
      Color.Linear color,
      Color.LinearDelta delta_color,
      Vector3fc particle_radius,
      Vector3fc growth_rate,
      float energy,
      float friction,
      int src_blend_func,
      int dst_blend_func,
      TextureKey[] textures,
      SpriteKey[] sprite_renderers,
      int types,
      boolean randomizeRotation,
      boolean randomizeScale) {
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
        sprite_renderers,
        types);
    this.uv_angle = uv_angle;
    this.current_velocity = new Vector3f(velocity);
    this.angle_bound = angle_bound;
    this.angle_max_jump = angle_max_jump;
    this.randomizeRotation = randomizeRotation;
    this.randomizeScale = randomizeScale;
  }

  public RandomVelocityEmitter(
      World world,
      Vector3f position,
      float offset_z,
      float uv_angle,
      float emitter_radius,
      float emitter_height,
      float angle_bound,
      float angle_max_jump,
      int num_particles,
      float particles_per_second,
      Vector3fc velocity,
      Vector3fc acceleration,
      Color.Linear color,
      Color.LinearDelta delta_color,
      Vector3fc particle_radius,
      Vector3fc growth_rate,
      float energy,
      float friction,
      int src_blend_func,
      int dst_blend_func,
      TextureKey[] textures,
      SpriteKey[] sprite_renderers,
      int types) {
    this(
        world,
        position,
        offset_z,
        uv_angle,
        emitter_radius,
        emitter_height,
        angle_bound,
        angle_max_jump,
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
        sprite_renderers,
        types,
        false,
        false);
  }

  public RandomVelocityEmitter(
      World world,
      Vector3f position,
      float offset_z,
      float uv_angle,
      float emitter_radius,
      float emitter_height,
      float angle_bound,
      float angle_max_jump,
      int num_particles,
      float particles_per_second,
      Vector3fc velocity,
      Vector3fc acceleration,
      Color.Linear color,
      Color.LinearDelta delta_color,
      Vector3fc particle_radius,
      Vector3fc growth_rate,
      float energy,
      float friction,
      int src_blend_func,
      int dst_blend_func,
      TextureKey[] textures) {
    this(
        world,
        position,
        offset_z,
        uv_angle,
        emitter_radius,
        emitter_height,
        angle_bound,
        angle_max_jump,
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
        textures.length,
        false,
        false);
  }

  public RandomVelocityEmitter(
      World world,
      Vector3f position,
      float offset_z,
      float emitter_radius,
      float emitter_height,
      float angle_bound,
      float angle_max_jump,
      int num_particles,
      float particles_per_second,
      Vector3fc velocity,
      Vector3fc acceleration,
      Color.Linear color,
      Color.LinearDelta delta_color,
      Vector3fc particle_radius,
      Vector3fc growth_rate,
      float energy,
      float friction,
      SpriteKey[] sprite_renderers) {
    this(
        world,
        position,
        offset_z,
        emitter_radius,
        emitter_height,
        angle_bound,
        angle_max_jump,
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
        sprite_renderers,
        false,
        false);
  }

  public RandomVelocityEmitter(
      World world,
      Vector3f position,
      float offset_z,
      float emitter_radius,
      float emitter_height,
      float angle_bound,
      float angle_max_jump,
      int num_particles,
      float particles_per_second,
      Vector3fc velocity,
      Vector3fc acceleration,
      Color.Linear color,
      Color.LinearDelta delta_color,
      Vector3fc particle_radius,
      Vector3fc growth_rate,
      float energy,
      float friction,
      SpriteKey[] sprite_renderers,
      boolean randomizeRotation,
      boolean randomizeScale) {
    this(
        world,
        position,
        offset_z,
        0f,
        emitter_radius,
        emitter_height,
        angle_bound,
        angle_max_jump,
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
        0,
        0,
        null,
        sprite_renderers,
        sprite_renderers.length,
        randomizeRotation,
        randomizeScale);
  }

  @Override
  protected int initParticle(
      Vector3f position,
      Vector3fc velocity,
      Vector3fc acceleration,
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
    randomizeVelocity();

    float angle = randomizeRotation ? (float) ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI) : uv_angle;
    LinearParticle particle = new LinearParticle(angle);
    if (randomizeRotation) {
      particle.setAngularVelocity(ThreadLocalRandom.current().nextFloat(-10f, 10f));
    }
    Vector3f pos = randomPosition();
    particle.setPos(pos.x(), pos.y(), pos.z());
    particle.setVelocity(current_velocity.x(), current_velocity.y(), current_velocity.z());
    particle.setAcceleration(acceleration.x(), acceleration.y(), acceleration.z());
    particle.setColor(colorR, colorG, colorB, colorA);
    particle.setDeltaColor(deltaR, deltaG, deltaB, deltaA);
    if (randomizeScale) {
      float scale = ThreadLocalRandom.current().nextFloat(0.4f, 1.5f);
      particle.setRadius(particle_radius.x() * scale, particle_radius.y() * scale, particle_radius.z() * scale);
    } else {
      particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
    }
    particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
    particle.setEnergy(energy);
    particle.setType(nextType());
    add(particle);
    return 1;
  }

  private void randomizeVelocity() {
    Random random = ThreadLocalRandom.current();
    float halfJump = .5f * angle_max_jump;
    float dx_angle = halfJump > 0f ? random.nextFloat(-halfJump, halfJump) : 0f;
    float dy_angle = halfJump > 0f ? random.nextFloat(-halfJump, halfJump) : 0f;

    if ((x_angle + dx_angle < -angle_bound) || (x_angle + dx_angle > angle_bound)) x_angle -= dx_angle;
    else x_angle += dx_angle;

    if ((y_angle + dy_angle < -angle_bound) || (y_angle + dy_angle > angle_bound)) y_angle -= dy_angle;
    else y_angle += dy_angle;

    float x = velocity.x() + velocity.z() * (float) Math.sin(x_angle);
    float y = velocity.y() + velocity.z() * (float) Math.sin(y_angle);
    current_velocity.set(x, y, velocity.z());
  }
}
