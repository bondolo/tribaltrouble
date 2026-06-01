package com.oddlabs.tt.model;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.BoundingBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Model} that hosts an {@link Emitter} at a specific world coordinate.
 * Used for standalone particle effects like explosions or impact bursts.
 * Can itself be augmented with accessories.
 */
public class PointEmitterModel extends Model implements Animated {
    protected final @NonNull Emitter<?> emitter;
    private final @NonNull AnimationManager manager;
    private final float height;

    public PointEmitterModel(@NonNull World world, @NonNull Emitter<?> emitter) {
        this(world, emitter, world.getAnimationManagerGameTime());
    }

    public PointEmitterModel(@NonNull World world, @NonNull Emitter<?> emitter, @NonNull AnimationManager manager) {
        super(world);
        this.emitter = emitter;
        this.manager = manager;
        float x = emitter.getPosition().x();
        float y = emitter.getPosition().y();
        float z = emitter.getPosition().z();
        float ground = world.getHeightMap().getNearestHeight(x, y);
        this.height = z - ground;

        setPosition(x, y);
        setPositionZ(z);
        register();
        manager.registerAnimation(this);
    }

    @Override
    public void remove() {
        super.remove();
        manager.removeAnimation(this);
    }

    @Override
    public float getOffsetZ() {
        return height;
    }

    @Override
    protected void onReinsert() {
        float x = getPositionX();
        float y = getPositionY();
        float z = getPositionZ();
        setBounds(x, x, y, y, z, z);
    }

    @Override
    public void animate(float t) {
        emitter.getPosition().set(getPositionX(), getPositionY(), getPositionZ());
        emitter.animate(t);
        animateClientState(t);
    }

    @Override
    public boolean isFinished() {
        return emitter.isFinished();
    }

    @Override
    public final @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    @Override
    protected @NonNull BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }

    public final @NonNull Emitter<?> getEmitter() {
        return emitter;
    }
}
