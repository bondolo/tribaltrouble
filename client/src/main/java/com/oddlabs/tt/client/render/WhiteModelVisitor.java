package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;

import org.jspecify.annotations.Nullable;

/**
 * A specialized {@link ModelVisitor} that renders models with a neutral white team color.
 * Used as a base for custom visitors or for temporary visual effects.
 */
class WhiteModelVisitor<M extends Model> extends ModelVisitor<M> {
    private static final WhiteModelVisitor<Model> INSTANCE = new WhiteModelVisitor<>();

    @SuppressWarnings("unchecked")
    public static <M extends Model> WhiteModelVisitor<M> getInstance() {
        return (WhiteModelVisitor<M>) INSTANCE;
    }

    @Override
    public @Nullable SpriteKey getSpriteKey(ElementSceneContext<M> render_state) {
        return null;
    }

    @Override
    public Color getSelectionColor(ElementSceneContext<M> render_state) {
        return Color.Linear.WHITE;
    }

    @Override
    public Color getTeamColor(ElementSceneContext<M> render_state) {
        return Color.Linear.WHITE;
    }

    @Override
    public Selectable.VisualPattern getPattern(ElementSceneContext<M> render_state) {
        return Selectable.VisualPattern.NONE;
    }

    @Override
    public void getTransform(ElementSceneContext<M> render_state, Matrix4f dest) {
        Model model = render_state.getModel();
        float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
        dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                .rotate(angle, 0f, 0f, 1f);
    }
}
