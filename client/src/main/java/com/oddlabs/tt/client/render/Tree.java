package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.SpriteList;
import org.jspecify.annotations.Nullable;

/**
 * Trunk and crown sprite lists representing a tree.
 */
record Tree(SpriteList trunk, SpriteList crown) {
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof Tree other_tree && crown == other_tree.crown && trunk == other_tree.trunk;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(trunk) * 31 + System.identityHashCode(crown);
    }
}
