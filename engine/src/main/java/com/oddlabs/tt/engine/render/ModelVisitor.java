package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;

import org.jspecify.annotations.Nullable;

/**
 * Visitor interface for applying specific logic to different types of models during world visitation.
 */
public abstract class ModelVisitor<M extends Model> {
    public void markDetailPoint(ElementSceneContext<M> render_state) {
        render_state.sceneContext.getRenderQueues().getMapDotRenderer().add(render_state);
    }

    public void addToRenderList(ElementSceneContext<M> render_state) {
        SpriteKey sprite = getSpriteKey(render_state);
        if (sprite != null) {
            M model = render_state.model;
            render_state.getRenderer(sprite).addToRenderList(render_state,
                    render_state.sceneContext.isResponding(model));
        }
    }

    public abstract @Nullable SpriteKey getSpriteKey(ElementSceneContext<M> render_state);

    public abstract void getTransform(ElementSceneContext<M> render_state, Matrix4f dest);

    public abstract Color getTeamColor(ElementSceneContext<M> render_state);

    public abstract Color getSelectionColor(ElementSceneContext<M> render_state);

    public abstract VisualPattern getPattern(ElementSceneContext<M> render_state);

    public float getNoDetailSize(ElementSceneContext<M> render_state) {
        return 0f;
    }
}
