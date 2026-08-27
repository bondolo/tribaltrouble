package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.engine.render.LightningAccessory;
import com.oddlabs.tt.engine.render.SonicBlastAccessory;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.simulation.model.EmojiType;
import com.oddlabs.tt.simulation.model.Model;

import java.util.ArrayList;
import java.util.SequencedCollection;

/**
 * Manages the client-side visual accessories for a simulation model.
 */
public final class VisualModel implements AutoCloseable {
    public static final float DURATION_CHICKEN_CLUCK = 0.8f;
    public static final float DURATION_UNIT_DEATH = 1.5f;
    public static final float DURATION_HARVEST = 1.0f;
    public static final float DURATION_REPAIR = 1.0f;

    private final Model model;
    private final SequencedCollection<Accessory> accessories = new ArrayList<>();

    public VisualModel(Model model) {
        this.model = model;
    }

    public SequencedCollection<Accessory> getAccessories() {
        return accessories;
    }

    public Model getModel() {
        return model;
    }

    public boolean isExpired() {
        if (accessories.isEmpty()) {
            return true;
        }
        for (Accessory acc : accessories) {
            if (!acc.isExpired()) {
                return false;
            }
        }
        return true;
    }

    public void update(float t) {
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

    public void addVisualSound(EmojiType emoji, float duration, float audioDistance) {
        AssetRegistry.getInstance().getEmojiSprite(emoji)
                .map(sprite -> new VisualSoundAccessory(sprite, duration, audioDistance))
                .ifPresent(accessories::add);
    }

    public void addLightningStrike(float targetX, float targetY, float targetZ) {
        for (Accessory acc : accessories) {
            if (acc instanceof LightningAccessory cloudAcc) {
                cloudAcc.triggerStrike(targetX, targetY, targetZ);
            }
        }
    }

    public void addSonicBlast(float targetX, float targetY, float targetZ, float radius, float duration) {
        for (Accessory acc : accessories) {
            if (acc instanceof SonicBlastAccessory blastAcc) {
                blastAcc.triggerBlast(targetX, targetY, targetZ, radius, duration);
            }
        }
    }
}
