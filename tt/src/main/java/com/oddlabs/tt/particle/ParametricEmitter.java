package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.RenderTools;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.tt.util.BoundingBox;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * An emitter that spawns particles whose movement is defined by a parametric function.
 */
public class ParametricEmitter extends Emitter<ParametricParticle> {
    private static final float SQRT_2 = (float) Math.sqrt(2f);
    private final @NonNull Random random;
    private final Vector3f randomized_offset = new Vector3f();
    private final @NonNull ParametricFunction function;
    private final float area_xy;
    private final float area_z;
    private final float velocity_u;
    private final float velocity_v;
    private final float velocity_random_margin;
    private final @NonNull Vector4fc color;
    private final @NonNull Vector3fc particle_radius;
    private final @NonNull Vector3fc growth_rate;
    private final BoundingBox bounds = new BoundingBox();

    private @NonNull Vector4fc delta_color;
    private float energy;

    public ParametricEmitter(@NonNull World world, @NonNull ParametricFunction function, @NonNull Vector3f position,
                             float area_xy, float area_z, float velocity_u, float velocity_v, float velocity_random_margin,
                             int num_particles, float particles_per_second,
                             @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
                             @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy,
                             int src_blend_func, int dst_blend_func, TextureKey @NonNull [] textures) {
        super(world, position, src_blend_func, dst_blend_func, textures, null, textures.length, num_particles, particles_per_second);
        this.function = function;
        this.area_xy = area_xy;
        this.area_z = area_z;
        this.velocity_u = velocity_u;
        this.velocity_v = velocity_v;
        this.velocity_random_margin = velocity_random_margin;
        this.color = color;
        this.delta_color = delta_color;
        this.particle_radius = particle_radius;
        this.growth_rate = growth_rate;
        this.energy = energy;
        random = world.getRandom();
    }

    public final void setDeltaColor(@NonNull Vector4f delta_color) {
        this.delta_color = delta_color;
    }

    public final void setEnergy(float energy) {
        this.energy = energy;
    }

    @Override
    public final void animate(float t) {
        updateSpawning(t);

        float x_min = Float.POSITIVE_INFINITY;
        float x_max = Float.NEGATIVE_INFINITY;
        float y_min = Float.POSITIVE_INFINITY;
        float y_max = Float.NEGATIVE_INFINITY;
        float z_min = Float.POSITIVE_INFINITY;
        float z_max = Float.NEGATIVE_INFINITY;

        for (List<ParametricParticle> list : getParticles()) {
            Iterator<ParametricParticle> particles = list.iterator();
            while (particles.hasNext()) {
                ParametricParticle particle = particles.next();
                if (particle.getEnergy() <= 0f) {
                    particles.remove();
                    continue;
                }

                particle.update(t, getScaleX(), getScaleY(), getScaleZ());
                float x = particle.getPosX();
                float y = particle.getPosY();
                float z = particle.getPosZ();
                particle.setPos(getX() + x, getY() + y, getZ() + z);

                x = particle.getPosX();
                y = particle.getPosY();
                z = particle.getPosZ();
                float radius_x = particle.getRadiusX() * SQRT_2;
                float radius_y = particle.getRadiusY() * SQRT_2;
                float radius_z = particle.getRadiusZ() * SQRT_2;
                x_min = Math.min(x_min, x - radius_x);
                x_max = Math.max(x_max, x + radius_x);
                y_min = Math.min(y_min, y - radius_y);
                y_max = Math.max(y_max, y + radius_y);
                z_min = Math.min(z_min, z - radius_z);
                z_max = Math.max(z_max, z + radius_z);
            }
        }
        bounds.setBounds(x_min, x_max, y_min, y_max, z_min, z_max);
    }

    @Override
    public void debugRender() {
        RenderTools.draw(bounds, 1f, 1f, 1f);
    }

    @Override
    protected int initParticles(int count) {
        int initiated = 0;
        for (int i = 0; i < count; i++) {
            initiated += initParticle(function, velocity_u, velocity_v, color, delta_color, particle_radius, growth_rate, energy);
        }
        return initiated;
    }

    protected int initParticle(@NonNull ParametricFunction function,
                               float velocity_u, float velocity_v,
                               @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
                               @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate,
                               float energy) {

        Vector3f offset = randomOffset(area_xy, area_xy, area_z);
        ParametricParticle particle = new ParametricParticle(getWorld(), function, random.nextFloat() * (float) Math.PI * 2f, random.nextFloat() * (float) Math.PI * 2f,
                offset.x(), offset.y(), offset.z());
        offset = randomOffset(velocity_random_margin, velocity_random_margin, 0f);
        particle.setVelocity(velocity_u + offset.x(), velocity_v + offset.y());
        particle.setColor(color.x(), color.y(), color.z(), color.w());
        particle.setDeltaColor(delta_color.x(), delta_color.y(), delta_color.z(), delta_color.w());
        particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
        particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
        particle.setEnergy(energy);
        particle.setType(random.nextInt(getTypes()));
        particle.update(0);
        add(particle);
        return 1;
    }

    protected final @NonNull Vector3f randomOffset(float a, float b, float c) {
        float x = random.nextFloat() * 2 * a - a;
        float y = random.nextFloat() * 2 * b - b;
        float z = random.nextFloat() * 2 * c - c;

        randomized_offset.set(x, y, z);
        return randomized_offset;
    }
}
