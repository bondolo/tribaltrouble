package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.AbstractElementNode;
import com.oddlabs.tt.simulation.model.ElementNode;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Plants;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.SceneryModel;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.tt.simulation.model.Target;
import java.util.List;
import org.joml.FrustumIntersection;

/// Traverses the world element quadtree to collect pickable targets inside a picking frustum.
final class ElementPicker {
  private static final float SUPPLY_PICK_HEIGHT = 2.0f;
  private static final float MIN_SUPPLY_PICK_RADIUS = 1.0f;

  private ElementPicker() {}

  static void pick(World world, CameraState camera, List<Target> results) {
    pick(world, camera, world.getInterpolationAlpha(), results);
  }

  static void pick(World world, CameraState camera, float interpolationAlpha, List<Target> results) {
    visit(world.getElementRoot(), camera.inNoDetailMode(), world, camera, interpolationAlpha, results);
  }

  private static void visit(
      AbstractElementNode<Model> node,
      boolean fullyInside,
      World world,
      CameraState camera,
      float interpolationAlpha,
      List<Target> results) {
    boolean childInside = fullyInside;
    if (!fullyInside) {
      int res = node.intersectFrustum(camera.getFrustum());
      if (res >= 0) {
        return;
      }
      childInside = (res == FrustumIntersection.INSIDE);
    }
    for (Model model = node.getModels().getFirst(); model != null; model = model.getNext()) {
      pickElement(model, childInside, world, camera, interpolationAlpha, results);
    }
    if (node instanceof ElementNode<Model> elementNode) {
      for (AbstractElementNode<Model> child : elementNode.children()) {
        visit(child, childInside, world, camera, interpolationAlpha, results);
      }
    }
  }

  private static void pickElement(
      Model model,
      boolean visibleOverride,
      World world,
      CameraState camera,
      float interpolationAlpha,
      List<Target> results) {
    if (!model.isRegistered() || model.isFinished()) {
      return;
    }
    switch (model) {
      case Plants _ -> {}
      case SceneryModel scenery -> {
        if (visibleOverride || scenery.testFrustum(camera.getFrustum())) {
          results.add(scenery);
        }
      }
      case Selectable<?> selectable -> {
        if (!selectable.isEnabled()) {
          return;
        }
        float x = selectable.getInterpolatedPositionX(interpolationAlpha);
        float y = selectable.getInterpolatedPositionY(interpolationAlpha);
        float zOffset = world.getHeightMap().computeInterpolatedHeight(0, x, y) + selectable.getOffsetZ();
        if (visibleOverride
            || inFrustum(
                camera.getFrustum(), x, y, zOffset, selectable.getSelectionRadius(), selectable.getSelectionHeight())) {
          results.add(selectable);
        }
      }
      case RubberSupply rubber -> {
        float x = rubber.getInterpolatedPositionX(interpolationAlpha);
        float y = rubber.getInterpolatedPositionY(interpolationAlpha);
        float zOffset = world.getHeightMap().computeInterpolatedHeight(0, x, y) + rubber.getOffsetZ();
        if (inFrustum(
            camera.getFrustum(),
            x,
            y,
            zOffset,
            Math.max(MIN_SUPPLY_PICK_RADIUS, rubber.getSize()),
            SUPPLY_PICK_HEIGHT)) {
          results.add(rubber);
        }
      }
      case SupplyModel supply -> {
        float zOffset = world.getHeightMap().computeInterpolatedHeight(0, supply.getPositionX(), supply.getPositionY());
        if (inFrustum(
            camera.getFrustum(),
            supply.getPositionX(),
            supply.getPositionY(),
            zOffset,
            Math.max(MIN_SUPPLY_PICK_RADIUS, supply.getSize()),
            SUPPLY_PICK_HEIGHT)) {
          results.add(supply);
        }
      }
      default -> {}
    }
  }

  private static boolean inFrustum(
      FrustumIntersection frustum,
      float posX,
      float posY,
      float zOffset,
      float selectionRadius,
      float selectionHeight) {
    return frustum.testAab(
        posX - selectionRadius,
        posY - selectionRadius,
        zOffset,
        posX + selectionRadius,
        posY + selectionRadius,
        zOffset + selectionHeight);
  }
}
