package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.util.BoundingBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Model} that can be augmented with {@link Accessory} objects.
 * Manages the lifecycle and animation of its attached accessories.
 */
public abstract class AccessorizableModel extends Model {
    private final List<@NonNull Accessory> attached_accessories = new ArrayList<>();

    protected AccessorizableModel(@NonNull World world) {
        super(world);
    }

    @Override
    protected @NonNull BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }


    /**
     * Ticks all animated accessories. Should be called by subclasses in their animate methods.
     *
     * @param t time delta since last frame.
     */
    protected final void animateAccessories(float t) {
        var it = attached_accessories.iterator();
        while (it.hasNext()) {
            Accessory accessory = it.next();
            if (accessory instanceof AnimatedAccessory animated) {
                animated.animate(t);
            }
            if (accessory.isExpired()) {
                it.remove();
            }
        }
    }

    public final void addAccessory(@NonNull Accessory accessory) {
        attached_accessories.add(accessory);
    }

    public final void removeAccessory(@NonNull Accessory accessory) {
        attached_accessories.remove(accessory);
    }

    public final @NonNull List<@NonNull Accessory> getAttachedAccessories() {
        return attached_accessories;
    }
}
