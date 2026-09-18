package com.oddlabs.tt.simulation.pathfinder;

import org.jspecify.annotations.Nullable;

/**
 * Immutable linked node representing a step along a grid path.
 *
 * @param parent previous node along the path
 * @param direction direction from previous node to this node
 */
public record GridPathNode(@Nullable GridPathNode parent, DirectionNode direction) implements PathNode<GridPathNode> {
}
