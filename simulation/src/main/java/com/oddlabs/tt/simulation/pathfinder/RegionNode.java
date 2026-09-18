package com.oddlabs.tt.simulation.pathfinder;

import org.jspecify.annotations.Nullable;

/**
 * Immutable linked node representing a step along a region path.
 *
 * @param parent previous node along the path
 * @param region region represented by this node
 */
public record RegionNode(@Nullable RegionNode parent, Region region) implements PathNode<RegionNode> {
}
