package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.simulation.model.EmojiType;
import com.oddlabs.tt.simulation.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;
import java.util.function.Predicate;

/**
 * Abstract base implementation of {@link VisualModel} providing common accessory management and lifecycle operations.
 */
public abstract class AbstractVisualModel implements VisualModel {
    private final Model model;
    private final SequencedCollection<Accessory> accessories = new ArrayList<>();

    protected AbstractVisualModel(Model model) {
        this.model = model;
    }

    protected final void addAccessory(Accessory accessory) {
        accessories.add(accessory);
    }

    protected final void removeAccessoriesIf(Predicate<Accessory> filter) {
        accessories.removeIf(filter);
    }

    @Override
    public SequencedCollection<Accessory> getAccessories() {
        if (this instanceof Accessory self) {
            if (accessories.isEmpty()) {
                return List.of(self);
            }
            List<Accessory> all = new ArrayList<>(1 + accessories.size());
            all.add(self);
            all.addAll(accessories);
            return Collections.unmodifiableList(all);
        }
        return Collections.unmodifiableSequencedCollection(accessories);
    }

    @Override
    public Model getModel() {
        return model;
    }

    /**
     * Determines whether the primary model or visual effect represented directly by this visual model has expired.
     *
     * @return true if the primary model/effect is dead or finished
     */
    protected boolean isSelfExpired() {
        return model.isDead();
    }

    @Override
    public boolean isExpired() {
        if (!isSelfExpired()) {
            return false;
        }
        for (Accessory acc : accessories) {
            if (!acc.isExpired()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void update(float t) {
        if (this instanceof AnimatedAccessory animated) {
            animated.animate(t);
        }
        boolean hasExpired = false;
        for (Accessory acc : accessories) {
            if (acc instanceof AnimatedAccessory animated) {
                animated.animate(t);
            }
            if (acc.isExpired()) {
                hasExpired = true;
            }
        }
        if (hasExpired) {
            for (Accessory acc : accessories) {
                if (acc.isExpired()) {
                    acc.close();
                }
            }
            accessories.removeIf(Accessory::isExpired);
        }
    }

    @Override
    public void close() {
        for (Accessory acc : accessories) {
            acc.close();
        }
        accessories.clear();
    }

    @Override
    public void addVisualSound(EmojiType emoji, float duration, float audioDistance) {
        AssetRegistry.getInstance().getEmojiSprite(emoji)
                .map(sprite -> new VisualSoundAccessory(sprite, duration, audioDistance))
                .ifPresent(accessories::add);
    }
}
