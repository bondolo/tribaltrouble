package com.oddlabs.tt.render;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.model.EmojiType;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.ModelClient;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the client-side visual state (accessories) for a simulation model.
 */
public final class VisualModel implements ModelClient {
    private static final java.util.Map<Integer, WeakReference<VisualModel>> activeVisualModels
            = new java.util.concurrent.ConcurrentHashMap<>();

    public static @Nullable VisualModel getById(int id) {
        WeakReference<VisualModel> ref = activeVisualModels.get(id);
        return ref != null ? ref.get() : null;
    }

    private final @NonNull Model model;
    private final @NonNull List<@NonNull Accessory> accessories = new ArrayList<>();
    private float visualOffsetZ = 0.0f;

    public float getVisualOffsetZ() {
        return visualOffsetZ;
    }

    public void setVisualOffsetZ(float visualOffsetZ) {
        this.visualOffsetZ = visualOffsetZ;
    }

    public VisualModel(@NonNull Model model) {
        this.model = model;
        activeVisualModels.put(model.getId(), new WeakReference<>(this));
    }

    public @NonNull List<@NonNull Accessory> getAccessories() {
        return accessories;
    }

    @Override
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
        activeVisualModels.remove(model.getId());
        for (Accessory acc : accessories) {
            acc.close();
        }
        accessories.clear();
    }

    @Override
    public void addVisualSound(@NonNull EmojiType emoji, float duration, float audioDistance) {
        VisualRegistry.getInstance().getEmojiSprite(emoji)
                .map(sprite -> new VisualSoundAccessory(sprite, duration, audioDistance))
                .ifPresent(accessories::add);
    }

    @Override
    public void addLightningStrike(float targetX, float targetY, float targetZ) {
        for (Accessory acc : accessories) {
            if (acc instanceof LightningCloudVisualAccessory cloudAcc) {
                cloudAcc.triggerStrike(targetX, targetY, targetZ);
            }
        }
    }

    @Override
    public void playSound(@NonNull AudioParameters params) {
        model.getWorld().getAudio().newAudio(model.getPositionX(), model.getPositionY(), model.getPositionZ(), params);
    }
}
