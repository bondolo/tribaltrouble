package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeLeaf;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import org.joml.FrustumIntersection;

/// Traverses the hierarchical tree spatial structure with frustum culling.
final class TreeTraversal {
  @FunctionalInterface
  interface Visitor {
    void visit(TreeSupply tree, boolean fullyInside);
  }

  private TreeTraversal() {}

  static void visit(AbstractTreeGroup root, CameraState camera, Visitor visitor) {
    visit(root, camera.inNoDetailMode(), camera.getFrustum(), visitor);
  }

  private static void visit(AbstractTreeGroup node, boolean fullyInside, FrustumIntersection frustum, Visitor visitor) {
    boolean childInside = fullyInside;
    if (!fullyInside) {
      int res = node.intersectFrustum(frustum);
      if (res >= 0) {
        return;
      }
      childInside = (res == FrustumIntersection.INSIDE);
    }
    switch (node) {
      case TreeGroup group -> {
        for (AbstractTreeGroup child : group.children()) {
          visit(child, childInside, frustum, visitor);
        }
      }
      case TreeLeaf leaf -> {
        for (TreeSupply tree : leaf.getTrees()) {
          visitor.visit(tree, childInside);
        }
      }
      case TreeSupply tree -> visitor.visit(tree, childInside);
    }
  }
}
