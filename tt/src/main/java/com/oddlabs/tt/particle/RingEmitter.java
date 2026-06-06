package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

/**
 * A linear emitter that spawns particles in a ring formation.
 */
public final class RingEmitter extends LinearEmitter {
    private final int num_particles;

    public RingEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height,
            int num_particles, float particles_per_second,
            @NonNull Vector3f velocity, @NonNull Vector3f acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3f particle_radius, @NonNull Vector3f growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            @NonNull TextureKey @NonNull [] textures) {
        super(world, position,
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
    protected int initParticle(@NonNull Vector3f position, @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy) {
        float angle = 2 * (float) Math.PI / num_particles;
        for (int i = 0; i < num_particles; i++) {
            LinearParticle particle = new LinearParticle(getWorld());
            Vector3f pos = position;
            particle.setPos(pos.x(), pos.y(), pos.z());
            // in this special case velocity.getZ() is the actual velocity. not the velocity in the z direction
            particle.setVelocity(velocity.z() * (float) Math.cos(angle * i), velocity.z() * (float) Math.sin(angle * i),
                    0);
            particle.setAcceleration(acceleration.x(), acceleration.y(), acceleration.z());
            particle.setColor(color);
            particle.setDeltaColor(delta_color);
            particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
            particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
            particle.setEnergy(energy);
            particle.setType(nextType());
            add(particle);
        }
        return num_particles;
    }
}
