package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundingBox;
import org.joml.Matrix4f;

/// Client-side visual state for a tree supply, managing its visual transform and render bounds.
record TreeRenderState(Matrix4f matrix, BoundingBox visualBounds) {}
