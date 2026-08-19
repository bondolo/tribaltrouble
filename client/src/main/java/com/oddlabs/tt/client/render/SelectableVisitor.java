package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.ElementSceneContext;
import com.oddlabs.tt.engine.render.ModelVisitor;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * ModelVisitor implementation that resolves visual properties and sprite keys for selectable entities
 * (units/buildings).
 */
class SelectableVisitor<S extends Selectable<?>> extends ModelVisitor<S> {

    @Override
    public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementSceneContext<S> render_state) {
        Selectable<?> selectable = render_state.getModel();
        Race race = selectable.getOwnerNoCheck().getRaceInfo().getRaceType();
        if (selectable instanceof Unit unit) {
            return Optional.of(AssetRegistry.getInstance().getUnitSprite(race, unit.getTemplate()
                    .getVisualType()));
        } else if (selectable instanceof Building building) {
            BuildingType bvt = building.getTemplate().getBuildingType();
            var visuals = AssetRegistry.getInstance().getBuildingVisuals(race, bvt);
            return Optional.ofNullable(switch (building.getBuildStage()) {
                case UNPLACED -> null;
                case START -> visuals.start();
                case HALFBUILT -> visuals.halfbuilt();
                case BUILT -> visuals.built();
            });
        }
        return Optional.empty();
    }

    @Override
    public void getTransform(@NonNull ElementSceneContext<S> render_state, @NonNull Matrix4f dest) {
        Model model = render_state.model;
        float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
        dest.translation(model.getPositionX(), model.getPositionY(), render_state.f)
                .rotate(angle, 0f, 0f, 1f);
    }

    public static Color.@NonNull Linear getTeamColor(@NonNull Selectable<?> model) {
        return model.getOwner().getColor();
    }

    @Override
    public final Color.@NonNull Linear getTeamColor(@NonNull ElementSceneContext<S> render_state) {
        return getTeamColor(render_state.getModel());
    }

    @Override
    public final @NonNull Color getSelectionColor(@NonNull ElementSceneContext<S> render_state) {
        RenderState renderState = (RenderState) render_state.sceneContext;
        Player local_player = renderState.getLocalPlayer();
        S model = render_state.getModel();
        return model.getSelectionColor(local_player, renderState.isSelected(model),
                renderState.isHovered(model));
    }

    @Override
    public final Selectable.@NonNull VisualPattern getPattern(@NonNull ElementSceneContext<S> render_state) {
        RenderState renderState = (RenderState) render_state.sceneContext;
        Player local_player = renderState.getLocalPlayer();
        return render_state.getModel().getVisualPattern(local_player);
    }

    @Override
    public final void markDetailPoint(@NonNull ElementSceneContext<S> render_state) {
        S selectable = render_state.model;
        if (!selectable.isDead())
            super.markDetailPoint(render_state);
    }
}
