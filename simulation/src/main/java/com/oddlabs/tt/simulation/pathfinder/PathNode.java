package com.oddlabs.tt.simulation.pathfinder;

import org.jspecify.annotations.Nullable;

/**
 * Traversal node in a linked path hierarchy.
 *
 * @param <P> the self-referential path node type
 */
public interface PathNode<P extends PathNode<P>> {
    @Nullable
    PathNode<P> parent();
}
