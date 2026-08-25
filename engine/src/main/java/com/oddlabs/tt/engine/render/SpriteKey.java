package com.oddlabs.tt.engine.render;


import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.geom.BoundsProvider;

/**
 * A render queue key identifying a sprite list, holding local bounds and animation types.
 */
public record SpriteKey(
                        int key,
                        BoundingBox[] bounds,
                        AnimationInfo.AnimationType[] animTypes
) implements RenderQueueKey, BoundsProvider {
}
