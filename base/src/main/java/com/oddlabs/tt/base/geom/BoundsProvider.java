package com.oddlabs.tt.base.geom;


/**
 * A simulation-side provider of physical entity bounding boxes.
 * Implemented by render-side SpriteKeys to supply bounds without render dependencies.
 */
public interface BoundsProvider {
    BoundingBox[] bounds();
}
