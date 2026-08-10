package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.effects.render.*;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Caches render state objects for reuse.
 */
final class RenderStateCache<RS extends LODObject> {
    private final @NonNull RenderStateFactory<RS> factory;
    private final List<@NonNull RS> cache = new ArrayList<>();
    private int current_index;

    public RenderStateCache(@NonNull RenderStateFactory<@NonNull RS> factory) {
        this.factory = factory;
    }

    public void clear() {
        current_index = 0;
    }

    public @NonNull RS get() {
        if (current_index == cache.size()) {
            cache.add(factory.create());
        }
        return cache.get(current_index++);
    }
}
