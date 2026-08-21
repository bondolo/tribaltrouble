package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;

import java.util.Optional;

/**
 * Visitor interface for applying specific logic to different types of models during world visitation.
 */
public abstract class ModelVisitor<M extends Model> {
    public void markDetailPoint(ElementSceneContext<M> render_state) {
        getSpriteKey(render_state).ifPresent(sprite -> render_state.getRenderer(sprite).addToNoDetailList(
                render_state));
    }

    public void markDetailPolygon(ElementSceneContext<M> render_state, PolyDetail detail) {
        getSpriteKey(render_state).ifPresent(sprite -> {
            M model = render_state.model;
            render_state.getRenderer(sprite).addToRenderList(detail, render_state,
                    render_state.sceneContext.isResponding(model));
        });
    }

    public final int getTriangleCount(ElementSceneContext<M> render_state, PolyDetail detail) {
        return getSpriteKey(render_state)
                .map(sprite -> render_state.getRenderer(sprite).getTriangleCount(detail))
                .orElse(0);
    }

    public final float getEyeDistanceSquared(ElementSceneContext<M> render_state) {
        M model = render_state.model;
        CameraState camera = render_state.sceneContext.getCamera();
        if (camera == null) {
            return 0f;
        }
        return RenderTools.getEyeDistanceSquared(model, camera.getCurrentX(), camera.getCurrentY(), camera
                .getCurrentZ());
    }

    public abstract Optional<SpriteKey> getSpriteKey(ElementSceneContext<M> render_state);

    public abstract void getTransform(ElementSceneContext<M> render_state, Matrix4f dest);

    public abstract Color getTeamColor(ElementSceneContext<M> render_state);

    public abstract Color getSelectionColor(ElementSceneContext<M> render_state);

    public abstract Selectable.VisualPattern getPattern(ElementSceneContext<M> render_state);
}
