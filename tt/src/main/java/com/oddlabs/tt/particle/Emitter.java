package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Base class for all particle emitters. Manages a collection of particles and their textures.
 * Handles the spawning logic and lifecycle (Budgeted or Infinite).
 */
public abstract class Emitter<P extends Particle> implements Animated {
    private final @NonNull List<@NonNull P> @NonNull [] particles;
    private final @NonNull TextureKey @Nullable [] textures;
    private final @NonNull SpriteKey @Nullable [] sprite_renderers;
    private final int src_blend_func;
    private final int dst_blend_func;
    private final int types;
    private final @NonNull World world;

    private @NonNull Vector3f position;
    private float scale_x = 1f;
    private float scale_y = 1f;
    private float scale_z = 1f;

    // Spawning State
    private float particles_per_second;
    /** Budget of particles to spawn. -1 for infinite. */
    private int remaining_particles;
    private float particle_counter = 0;
    private boolean started = true;

    @SuppressWarnings("unchecked")
    public Emitter(@NonNull World world, @NonNull Vector3f position,
            int src_blend_func, int dst_blend_func,
            @NonNull TextureKey @Nullable [] textures, @NonNull SpriteKey @Nullable [] sprite_renderers,
            int types, int remaining_particles, float particles_per_second) {
        this.world = world;
        this.position = position;
        this.src_blend_func = src_blend_func;
        this.dst_blend_func = dst_blend_func;
        this.textures = textures;
        this.sprite_renderers = sprite_renderers;
        this.types = types;
        this.remaining_particles = remaining_particles;
        this.particles_per_second = particles_per_second;
        particles = Stream.generate(ArrayList::new).limit(types).toArray(List[]::new);
    }

    public final @NonNull World getWorld() {
        return world;
    }

    public final @NonNull SpriteKey @Nullable [] getSpriteRenderers() {
        return sprite_renderers;
    }

    public final List<@NonNull P> @NonNull [] getParticles() {
        return particles;
    }

    public final @NonNull TextureKey @Nullable [] getTextures() {
        return textures;
    }

    public final int getSrcBlendFunc() {
        return src_blend_func;
    }

    public final int getDstBlendFunc() {
        return dst_blend_func;
    }

    /**
     * Returns true if there are any particles currently active in this emitter.
     *
     * @return true if there are active particles.
     */
    public final boolean hasActiveParticles() {
        return Arrays.stream(particles).anyMatch(list -> !list.isEmpty());
    }

    protected final void add(@NonNull P particle) {
        particles[particle.getType()].add(particle);
    }

    public final void setPosition(@NonNull Vector3f position) {
        this.position = position;
    }

    public final @NonNull Vector3f getPosition() {
        return position;
    }

    final float getX() {
        return position.x();
    }

    final float getY() {
        return position.y();
    }

    final float getZ() {
        return position.z();
    }

    public final void scale(float scale_x, float scale_y, float scale_z) {
        this.scale_x = scale_x;
        this.scale_y = scale_y;
        this.scale_z = scale_z;
    }

    public final float getScaleX() {
        return scale_x;
    }

    public final float getScaleY() {
        return scale_y;
    }

    public final float getScaleZ() {
        return scale_z;
    }

    public final void adjustColor(Color.@NonNull LinearDelta delta) {
        Arrays.stream(particles)
                .flatMap(List::stream)
                .forEach(p -> p.setColor(p.getColor().add(delta)));
    }

    public final void start() {
        started = true;
    }

    public final void stop() {
        started = false;
    }

    public final boolean isStarted() {
        return started;
    }

    /**
     * Stops any further spawning. Existing particles will continue to live until they run out of energy.
     */
    public final void done() {
        remaining_particles = 0;
    }

    protected final int getRemainingParticles() {
        return remaining_particles;
    }

    public final float getParticlesPerSecond() {
        return particles_per_second;
    }

    public final void setParticlesPerSecond(float particles_per_second) {
        this.particles_per_second = particles_per_second;
    }

    protected final void setRemainingParticles(int remaining) {
        this.remaining_particles = remaining;
    }

    /**
     * Core spawning loop. Should be called in {@link #animate(float t)}.
     */
    protected final void updateSpawning(float t) {
        if (started)
            particle_counter += particles_per_second * t;

        while (particle_counter >= 1 && (remaining_particles == -1 || remaining_particles != 0) && started) {
            int initiated = initParticles(1);
            assert initiated <= remaining_particles || remaining_particles == -1 : "Too many particles initiated";
            particle_counter -= initiated;
            if (remaining_particles > 0)
                remaining_particles = Math.max(0, remaining_particles - initiated);
        }
    }

    /**
     * Subclasses must implement this to initialize and add new particles.
     *
     * @param count Requested number of particles to spawn (usually 1 based on updateSpawning loop).
     * @return The actual number of particles spawned.
     */
    protected abstract int initParticles(int count);

    protected final int getTypes() {
        return types;
    }

    @Override
    public final void updateChecksum(@NonNull StateChecksum checksum) {
    }

    /**
     * Returns true if the emitter has finished spawning and all its particles have died.
     *
     * @return true if the emitter is finished.
     */
    public final boolean isFinished() {
        return remaining_particles == 0 && !hasActiveParticles();
    }

    /**
     * Renders debug information for this emitter.
     */
    public void debugRender() {
    }
}
