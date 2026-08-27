package com.oddlabs.tt.effects.particle;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.util.Color;
import org.joml.Vector3fc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Visual effect representing a lightning strike.
 */
public final class Lightning implements Animated, BoundsProvider {
    private static final float SQRT_2 = (float) Math.sqrt(2f);

    private final Deque<StretchParticle> particles = new ArrayDeque<>();
    private final Vector3fc src;
    private final Vector3fc dst;
    private final float width;
    private final int num_particles;
    private final Color.Linear color;
    private final Color.LinearDelta delta_color;
    private final TextureKey texture;
    private final float energy;
    private final BoundingBox bounds = new BoundingBox();
    private final BoundingBox[] boundsArray = new BoundingBox[]{bounds};

    public Lightning(Vector3fc src, Vector3fc dst, float width,
            int num_particles, Color.Linear color, Color.LinearDelta delta_color,
            TextureKey texture, float energy) {
        this.src = src;
        this.dst = dst;
        this.width = width;
        this.num_particles = num_particles;
        this.color = color;
        this.delta_color = delta_color;
        this.texture = texture;
        this.energy = energy;
        initParticles();
    }

    @Override
    public BoundingBox[] bounds() {
        return boundsArray;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public Deque<StretchParticle> getParticles() {
        return particles;
    }

    public TextureKey getTexture() {
        return texture;
    }

    private void initParticles() {
        Random random = ThreadLocalRandom.current();
        float x = src.x();
        float y = src.y();
        float z = src.z();
        float height = dst.z() - src.z();
        float random_limit = Math.abs(height) / 6f;
        float dz = (height) / num_particles;

        for (int i = 0; i < num_particles; i++) {
            float base_dx = (dst.x() - x) / (num_particles - i);
            float base_dy = (dst.y() - y) / (num_particles - i);
            float halfLimit = 0.5f * random_limit;
            float dx = base_dx + (halfLimit > 0f ? random.nextFloat(-halfLimit, halfLimit) : 0f);
            float dy = base_dy + (halfLimit > 0f ? random.nextFloat(-halfLimit, halfLimit) : 0f);
            StretchParticle particle = new StretchParticle();
            particle.setSrc(x, y, z);

            if (i == num_particles - 1) {
                x = dst.x();
                y = dst.y();
                z = dst.z();
                particle.setDstWidth(width / 2);
            } else {
                x += dx;
                y += dy;
                z += dz;
                particle.setDstWidth(width);
            }
            particle.setDst(x, y, z);
            initParticle(particle);
            particles.add(particle);
        }
    }

    private void initParticle(StretchParticle particle) {
        particle.setSrcWidth(width);
        particle.setColor(color);
        particle.setDeltaColor(delta_color);
        particle.setRadius(0f, 0f, 0f);
        particle.setGrowthRate(0f, 0f, 0f);
        particle.setEnergy(energy);
    }

    @Override
    public void animate(float t) {
        float x_min = Float.POSITIVE_INFINITY;
        float x_max = Float.NEGATIVE_INFINITY;
        float y_min = Float.POSITIVE_INFINITY;
        float y_max = Float.NEGATIVE_INFINITY;
        float z_min = Float.POSITIVE_INFINITY;
        float z_max = Float.NEGATIVE_INFINITY;

        for (StretchParticle particle : particles) {
            particle.update(t);
            float x = particle.getSrcX();
            float y = particle.getSrcY();
            float z = particle.getSrcZ();
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
        particles.removeIf(StretchParticle::isDead);
        bounds.setBounds(x_min, x_max, y_min, y_max, z_min, z_max);
    }

    public boolean isFinished() {
        return particles.isEmpty();
    }
}
