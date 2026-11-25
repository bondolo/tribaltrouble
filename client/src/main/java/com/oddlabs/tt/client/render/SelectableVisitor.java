package com.oddlabs.tt.client.render;

import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.engine.render.ElementSceneContext;
import com.oddlabs.tt.engine.render.ModelVisitor;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.VisualPattern;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/// ModelVisitor implementation that resolves visual properties and sprite keys for selectable entities
/// (units/buildings).
final class SelectableVisitor<S extends Selectable<?>> extends ModelVisitor<S> {
  private final AssetRegistry assets;

  SelectableVisitor(AssetRegistry assets) {
    this.assets = assets;
  }

  @Override
  public @Nullable SpriteKey getSpriteKey(ElementSceneContext<S> render_state) {
    Selectable<?> selectable = render_state.getModel();
    Race race = selectable.getOwnerNoCheck().getRaceInfo().getRaceType();
    return switch (selectable) {
      case Unit unit -> assets.getUnitSprite(race, unit.getTemplate().getUnitType());
      case Building building -> {
        var visuals = assets.getBuildingVisuals(race, building.getTemplate().getBuildingType());
        yield switch (building.getBuildStage()) {
          case UNPLACED -> null;
          case START -> visuals.start();
          case HALFBUILT -> visuals.halfbuilt();
          case BUILT -> visuals.built();
        };
      }
    };
  }

  @Override
  public void getTransform(ElementSceneContext<S> render_state, Matrix4f dest) {
    S model = render_state.model;
    float alpha = render_state.sceneContext.getInterpolationAlpha();
    float x = model.getInterpolatedPositionX(alpha);
    float y = model.getInterpolatedPositionY(alpha);
    float z =
        render_state.sceneContext instanceof RenderState rs
            ? rs.getVisuallyCorrectHeight(x, y) + model.getOffsetZ()
            : render_state.f;
    float angle = model.getInterpolatedAngle(alpha);
    dest.translation(x, y, z).rotate(angle, 0f, 0f, 1f);
  }

  public static Color.Linear getTeamColor(Selectable<?> model) {
    return model.getOwner().getColor();
  }

  @Override
  public final Color.Linear getTeamColor(ElementSceneContext<S> render_state) {
    return getTeamColor(render_state.getModel());
  }

  public static VisualPattern resolvePattern(Selectable<?> selectable, Player localPlayer) {
    boolean isBuilding = selectable instanceof Building;
    Player owner = selectable.getOwner();
    return owner == localPlayer
        ? isBuilding ? VisualPattern.FRIENDLY_BUILDING : VisualPattern.FRIENDLY
        : localPlayer.isEnemy(owner)
            ? isBuilding ? VisualPattern.ENEMY_BUILDING : VisualPattern.ENEMY
            : isBuilding ? VisualPattern.NEUTRAL_BUILDING : VisualPattern.NEUTRAL;
  }

  public static Color.Linear resolveSelectionColor(
      Selectable<?> selectable, Player localPlayer, boolean selected, boolean hovered) {
    VisualPattern pattern = resolvePattern(selectable, localPlayer);
    return selected ? pattern.selectedColor : hovered ? pattern.hoveredColor : selectable.getOwner().getColor();
  }

  @Override
  public final Color getSelectionColor(ElementSceneContext<S> render_state) {
    RenderState renderState = (RenderState) render_state.sceneContext;
    Player local_player = renderState.getLocalPlayer();
    S model = render_state.getModel();
    return resolveSelectionColor(model, local_player, renderState.isSelected(model), renderState.isHovered(model));
  }

  @Override
  public final VisualPattern getPattern(ElementSceneContext<S> render_state) {
    RenderState renderState = (RenderState) render_state.sceneContext;
    Player local_player = renderState.getLocalPlayer();
    return resolvePattern(render_state.getModel(), local_player);
  }

  @Override
  public final void markDetailPoint(ElementSceneContext<S> render_state) {
    S selectable = render_state.model;
    if (!selectable.isDead()) super.markDetailPoint(render_state);
  }

  @Override
  public float getNoDetailSize(ElementSceneContext<S> render_state) {
    Selectable<?> selectable = render_state.getModel();
    return switch (selectable) {
      case Unit _ -> 1.0f;
      case Building building -> {
        Race race = selectable.getOwnerNoCheck().getRaceInfo().getRaceType();
        yield assets.getBuildingVisuals(race, building.getTemplate().getBuildingType()).noDetailSize();
      }
    };
  }
}
