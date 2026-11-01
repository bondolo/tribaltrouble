package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;

import org.jspecify.annotations.Nullable;

/**
 * A specialized {@link ModelVisitor} that renders models with a neutral white team color.
 * Used as a base for custom visitors or for temporary visual effects.
 */
public class WhiteModelVisitor<M extends Model> extends ModelVisitor<M> {
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
        return Color.Linear.TRANSPARENT;
    }

    @Override
    public VisualPattern getPattern(ElementSceneContext<M> render_state) {
        return VisualPattern.NONE;
    }

    @Override
    public void getTransform(ElementSceneContext<M> render_state, Matrix4f dest) {
        Model model = render_state.getModel();
        float alpha = render_state.sceneContext.getInterpolationAlpha();
        float x = model.getInterpolatedPositionX(alpha);
        float y = model.getInterpolatedPositionY(alpha);
        float z = model.getInterpolatedPositionZ(alpha);
        float angle = model.getInterpolatedAngle(alpha);
        dest.translation(x, y, z)
                .rotate(angle, 0f, 0f, 1f);
    }
}
