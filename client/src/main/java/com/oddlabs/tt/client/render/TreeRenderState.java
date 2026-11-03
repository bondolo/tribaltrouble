package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import org.joml.Matrix4f;

/**
 * Client-side visual state for a tree supply, managing its visual transform and render bounds.
 */
record TreeRenderState(
                       TreeSupply tree,
                       Matrix4f matrix,
                       BoundingBox visualBounds
) {
}
