package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.render.SpriteList;

/**
 * Visual definition of a tree, including meshes, shadow properties, and bounds.
 */
record Tree(
        SpriteList trunk,
        SpriteList crown,
        float shadowDiameter,
        float shadowOpacity,
        float shadowVerticalCenter,
        float heightScale,
        BoundingBox modelBounds
) {
    Tree(SpriteList trunk, SpriteList crown, float shadowDiameter, float shadowOpacity,
            float shadowVerticalCenter, float heightScale) {
        this(trunk, crown, shadowDiameter, shadowOpacity, shadowVerticalCenter, heightScale,
                computeModelBounds(trunk, crown));
    }

    private static BoundingBox computeModelBounds(SpriteList trunk, SpriteList crown) {
        BoundingBox box = new BoundingBox();
        box.setBounds(trunk.getBounds()[0]);
        box.checkBounds(crown.getBounds()[0]);
        box.checkBoundsZ(0f);
        return box;
    }
}
