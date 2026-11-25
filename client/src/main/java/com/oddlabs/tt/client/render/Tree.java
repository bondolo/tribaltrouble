package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.render.SpriteList;

/// Visual definition of a tree, including meshes, shadow properties, and bounds.
record Tree(
    SpriteList trunk,
    SpriteList crown,
    float shadowDiameter,
    float shadowOpacity,
    float shadowVerticalCenter,
    BoundingBox modelBounds) {
  Tree(SpriteList trunk, SpriteList crown, float shadowDiameter, float shadowOpacity, float shadowVerticalCenter) {
    this(
        trunk,
        crown,
        shadowDiameter,
        shadowOpacity,
        shadowVerticalCenter,
        computeModelBounds(trunk.getBounds()[0], crown.getBounds()[0]));
  }

  private static BoundingBox computeModelBounds(BoundingBox trunkBounds, BoundingBox crownBounds) {
    BoundingBox box = new BoundingBox();
    box.setBounds(trunkBounds);
    box.expandBounds(crownBounds);
    box.expandBoundsZ(0f);
    return box;
  }
}
