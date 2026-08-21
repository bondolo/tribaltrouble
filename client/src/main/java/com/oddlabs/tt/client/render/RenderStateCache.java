package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.effects.render.*;


import java.util.ArrayList;
import java.util.List;

/**
 * Caches render state objects for reuse.
 */
final class RenderStateCache<RS extends LODObject> {
    private final RenderStateFactory<RS> factory;
    private final List<RS> cache = new ArrayList<>();
    private int current_index;

    public RenderStateCache(RenderStateFactory<RS> factory) {
        this.factory = factory;
    }

    public void clear() {
        current_index = 0;
    }

    public RS get() {
        if (current_index == cache.size()) {
            cache.add(factory.create());
        }
        return cache.get(current_index++);
    }
}
