package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.landscape.World;
import java.util.List;
import org.joml.FrustumIntersection;
import org.joml.Matrix4fc;

/// Traverses the world tree spatial hierarchy to collect pickable trees inside a picking frustum.
final class TreePicker {
  private static final float SELECTION_RADIUS = 1.5f;

  private TreePicker() {}

  static void pick(World world, CameraState camera, List<TreeSupply> results) {
    FrustumIntersection frustum = camera.getFrustum();
    LandscapeBoundsProvider boundsProvider = world.getLandscapeResources();
    TreeTraversal.visit(
        world.getTreeRoot(),
        camera,
        (tree, fullyInside) -> {
          if (tree.isDead()) {
            return;
          }
          BoundingBox modelBounds = boundsProvider.getTreeBounds(tree.getTreeType()).bounds()[0];
          if (fullyInside || inFrustum(tree, modelBounds, frustum)) {
            results.add(tree);
          }
        });
  }

  private static boolean inFrustum(TreeSupply tree, BoundingBox source, FrustumIntersection frustum) {
    Matrix4fc matrix = tree.getMatrix();
    float minZ = matrix.m32();
    float maxZ = minZ;
    float m02 = matrix.m02();
    if (m02 > 0f) {
      minZ += m02 * source.bmin_x;
      maxZ += m02 * source.bmax_x;
    } else {
      minZ += m02 * source.bmax_x;
      maxZ += m02 * source.bmin_x;
    }
    float m12 = matrix.m12();
    if (m12 > 0f) {
      minZ += m12 * source.bmin_y;
      maxZ += m12 * source.bmax_y;
    } else {
      minZ += m12 * source.bmax_y;
      maxZ += m12 * source.bmin_y;
    }
    float m22 = matrix.m22();
    if (m22 > 0f) {
      minZ += m22 * source.bmin_z;
      maxZ += m22 * source.bmax_z;
    } else {
      minZ += m22 * source.bmax_z;
      maxZ += m22 * source.bmin_z;
    }
    float posX = tree.getPositionX();
    float posY = tree.getPositionY();
    float heightScale = tree.getTreeType().pickHeightScale();
    return frustum.testAab(
        posX - SELECTION_RADIUS,
        posY - SELECTION_RADIUS,
        minZ,
        posX + SELECTION_RADIUS,
        posY + SELECTION_RADIUS,
        minZ + (maxZ - minZ) * heightScale);
  }
}
