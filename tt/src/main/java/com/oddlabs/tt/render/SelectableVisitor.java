package com.oddlabs.tt.render;

import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.tt.model.snapshot.VisualSnapshots.BuildingSnapshot;
import com.oddlabs.tt.model.snapshot.VisualSnapshots.UnitSnapshot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * ModelVisitor implementation that resolves visual properties and sprite keys for selectable entities
 * (units/buildings) from their snapshots.
 */
class SelectableVisitor<S extends EntitySnapshot> extends ModelVisitor<S> {

    @Override
    public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<S> render_state) {
        EntitySnapshot entity = render_state.getEntity();
        if (entity instanceof UnitSnapshot unit) {
            return Optional.of(VisualRegistry.getInstance().getUnitSprite(unit.race(), unit.visualType()));
        } else if (entity instanceof BuildingSnapshot building) {
            BuildingType bvt = building.buildingType();
            var visuals = VisualRegistry.getInstance().getBuildingVisuals(building.race(), bvt);
            return Optional.ofNullable(switch (building.buildStage()) {
                case UNPLACED -> null;
                case START -> visuals.start();
                case HALFBUILT -> visuals.halfbuilt();
                case BUILT -> visuals.built();
            });
        }
        return Optional.empty();
    }

    @Override
    public void getTransform(@NonNull ElementRenderState<S> render_state, @NonNull Matrix4f dest) {
        EntitySnapshot entity = render_state.getEntity();
        float angle = (float) Math.atan2(entity.dirY(), entity.dirX());
        dest.translation(entity.x(), entity.y(), render_state.f)
                .rotate(angle, 0f, 0f, 1f);
    }

    @Override
    public final Color.@NonNull Linear getTeamColor(@NonNull ElementRenderState<S> render_state) {
        EntitySnapshot entity = render_state.getEntity();
        if (entity instanceof UnitSnapshot unit) {
            return unit.teamColor();
        } else if (entity instanceof BuildingSnapshot building) {
            return building.teamColor();
        }
        return Color.Linear.WHITE;
    }

    private static Selectable.@NonNull VisualPattern getVisualPattern(@NonNull Player local_player,
            Color.@NonNull Linear teamColor, boolean isBuilding) {
        Player owner = null;
        for (Player p : local_player.getWorld().getPlayers()) {
            if (p.getColor().equals(teamColor)) {
                owner = p;
                break;
            }
        }
        if (owner == null) {
            return isBuilding ? Selectable.VisualPattern.NEUTRAL_BUILDING : Selectable.VisualPattern.NEUTRAL;
        }
        if (owner == local_player) {
            return isBuilding ? Selectable.VisualPattern.FRIENDLY_BUILDING : Selectable.VisualPattern.FRIENDLY;
        } else if (local_player.isEnemy(owner)) {
            return isBuilding ? Selectable.VisualPattern.ENEMY_BUILDING : Selectable.VisualPattern.ENEMY;
        } else {
            return isBuilding ? Selectable.VisualPattern.NEUTRAL_BUILDING : Selectable.VisualPattern.NEUTRAL;
        }
    }

    private static @NonNull Color getSelectionColor(@NonNull Player local_player, Color.@NonNull Linear teamColor,
            boolean isBuilding, boolean selected, boolean hovered) {
        Selectable.VisualPattern pattern = getVisualPattern(local_player, teamColor, isBuilding);
        return selected
                ? pattern.selectedColor
                : hovered
                        ? pattern.hoveredColor
                : teamColor;
    }

    @Override
    public final @NonNull Color getSelectionColor(@NonNull ElementRenderState<S> render_state) {
        Player local_player = render_state.render_state.getLocalPlayer();
        EntitySnapshot entity = render_state.getEntity();
        Color.Linear teamColor = Color.Linear.WHITE;
        boolean isBuilding = false;
        if (entity instanceof UnitSnapshot unit) {
            teamColor = unit.teamColor();
        } else if (entity instanceof BuildingSnapshot building) {
            teamColor = building.teamColor();
            isBuilding = true;
        }
        boolean selected = render_state.render_state.isSelected(entity);
        boolean hovered = render_state.render_state.isHovered(entity);
        return getSelectionColor(local_player, teamColor, isBuilding, selected, hovered);
    }

    @Override
    public final Selectable.@NonNull VisualPattern getPattern(@NonNull ElementRenderState<S> render_state) {
        Player local_player = render_state.render_state.getLocalPlayer();
        EntitySnapshot entity = render_state.getEntity();
        Color.Linear teamColor = Color.Linear.WHITE;
        boolean isBuilding = false;
        if (entity instanceof UnitSnapshot unit) {
            teamColor = unit.teamColor();
        } else if (entity instanceof BuildingSnapshot building) {
            teamColor = building.teamColor();
            isBuilding = true;
        }
        return getVisualPattern(local_player, teamColor, isBuilding);
    }

    @Override
    public final void markDetailPoint(@NonNull ElementRenderState<S> render_state) {
        EntitySnapshot entity = render_state.getEntity();
        boolean dead = false;
        if (entity instanceof UnitSnapshot unit) {
            dead = unit.isDead();
        }
        if (!dead) {
            super.markDetailPoint(render_state);
        }
    }
}
