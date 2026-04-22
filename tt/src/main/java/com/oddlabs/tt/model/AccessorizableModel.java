package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;

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

    /**
     * Ticks all animated accessories. Should be called by subclasses in their animate methods.
     * @param t time delta since last frame.
     */
    protected final void animateAccessories(float t) {
        for (var accessory : attached_accessories) {
            if (accessory instanceof AnimatedAccessory animated) {
                animated.animate(t);
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
