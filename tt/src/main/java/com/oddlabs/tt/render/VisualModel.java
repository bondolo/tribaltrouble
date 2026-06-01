package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.ModelClient;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Manages the client-side visual state (accessories) for a simulation model.
 */
public final class VisualModel implements ModelClient {
    private final @NonNull Model model;
    private final @NonNull List<@NonNull Accessory> accessories = new ArrayList<>();

    public VisualModel(@NonNull Model model) {
        this.model = model;
    }

    public @NonNull List<@NonNull Accessory> getAccessories() {
        return accessories;
    }

    @Override
    public void update(float t) {
        var it = accessories.iterator();
        while (it.hasNext()) {
            Accessory acc = it.next();
            if (acc instanceof AnimatedAccessory animated) {
                animated.animate(t);
            }
            if (acc.isExpired()) {
                it.remove();
            }
        }
    }

    @Override
    public void addVisualSound(@NonNull SpriteKey sprite, float duration, float audioDistance) {
        accessories.add(new VisualSoundAccessory(sprite, duration, audioDistance));
    }
}
