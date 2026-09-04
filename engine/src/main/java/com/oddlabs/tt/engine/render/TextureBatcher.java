package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.render.state.RenderContext;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Texture slot tracking in multi-texture batching renderers.
 */
final class TextureBatcher {
    private final int maxSlots;
    private final IdentityHashMap<Texture, Integer> textureToSlot = new IdentityHashMap<>();
    private final List<Texture> activeTextures;

    TextureBatcher(int maxSlots) {
        this.maxSlots = maxSlots;
        this.activeTextures = new ArrayList<>(maxSlots);
    }

    /**
     * Gets the current slot for a texture, or assigns a new one if available.
     * Returns -1 if the texture is not currently batched and the batch is full.
     */
    int getOrAssignSlot(Texture texture) {
        Integer slot = textureToSlot.get(texture);
        if (slot != null) {
            return slot;
        }
        if (activeTextures.size() >= maxSlots) {
            return -1; // Batch is full
        }
        slot = activeTextures.size();
        activeTextures.add(texture);
        textureToSlot.put(texture, slot);
        return slot;
    }

    /**
     * Binds all active textures to the provided RenderContext, starting at the specified offset unit.
     */
    void bindTextures(RenderContext context, int startUnit) {
        for (int i = 0; i < activeTextures.size(); i++) {
            context.setTexture(startUnit + i, activeTextures.get(i).getHandle());
        }
    }

    void clear() {
        textureToSlot.clear();
        activeTextures.clear();
    }
}
