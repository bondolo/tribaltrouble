package com.oddlabs.tt.effects.particle;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.BoundingBox;
import com.oddlabs.tt.simulation.model.Model;
import org.jspecify.annotations.Nullable;

/**
 * A visual scene-graph {@link Model} that hosts an {@link Emitter} at a specific world coordinate.
 * Used for standalone particle effects like explosions, rubble, or poison gas bursts.
 */
public class PointEmitterModel extends Model implements Animated {
    protected final Emitter<?> emitter;
    private final AnimationManager manager;

    /**
     * Constructs a new PointEmitterModel using the world's game-time animation manager.
     *
     * @param world the world
     * @param emitter the particle emitter to host
     */
    public PointEmitterModel(World world, Emitter<?> emitter) {
        this(world, emitter, world.getAnimationManagerGameTime());
    }

    /**
     * Constructs a new PointEmitterModel using the specified animation manager.
     *
     * @param world the world
     * @param emitter the particle emitter to host
     * @param manager the animation manager to register with
     */
    public PointEmitterModel(World world, Emitter<?> emitter,
            AnimationManager manager) {
        super(world);
        this.emitter = emitter;
        this.manager = manager;
        float x = emitter.getPosition().x();
        float y = emitter.getPosition().y();
        float z = emitter.getPosition().z();

        setPosition(x, y, z);
        register();
        manager.registerAnimation(this);
    }

    @Override
    public void remove() {
        super.remove();
        manager.removeAnimation(this);
    }

    @Override
    protected void onReinsert() {
        BoundingBox bounds = emitter.getBounds();
        if (bounds.isValid()) {
            setBounds(bounds.bmin_x, bounds.bmax_x, bounds.bmin_y, bounds.bmax_y, bounds.bmin_z, bounds.bmax_z);
        } else {
            float x = getPositionX();
            float y = getPositionY();
            float z = getPositionZ();
            setBounds(x, x, y, y, z, z);
        }
    }

    @Override
    public void animate(float t) {
        emitter.getPosition().set(getPositionX(), getPositionY(), getPositionZ());
        emitter.animate(t);
        reinsert();
        if (emitter.isFinished()) {
            remove();
        }
    }

    @Override
    public boolean isFinished() {
        return emitter.isFinished();
    }

    @Override
    protected BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }

    /**
     * Returns the underlying particle emitter hosted by this model.
     *
     * @return the hosted emitter
     */
    public final Emitter<?> getEmitter() {
        return emitter;
    }
}
