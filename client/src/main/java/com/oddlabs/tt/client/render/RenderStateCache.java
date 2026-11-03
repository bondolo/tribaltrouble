package com.oddlabs.tt.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Caches render state objects for reuse.
 */
final class RenderStateCache<RS> {
    private final Supplier<RS> factory;
    private final List<RS> cache = new ArrayList<>();
    private int current_index;

    public RenderStateCache(Supplier<RS> factory) {
        this.factory = factory;
    }

    public void clear() {
        current_index = 0;
    }

    public RS get() {
        if (current_index == cache.size()) {
            cache.add(factory.get());
        }
        return cache.get(current_index++);
    }
}
