package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Visitor interface for applying specific logic to different types of models during world visitation.
 */
abstract class ModelVisitor<S extends EntitySnapshot> {
    public void markDetailPoint(@NonNull ElementRenderState<S> render_state) {
        getSpriteKey(render_state).ifPresent(sprite -> render_state.getRenderer(sprite).addToNoDetailList(
                render_state));
    }

    public void markDetailPolygon(@NonNull ElementRenderState<S> render_state, @NonNull PolyDetail detail) {
        getSpriteKey(render_state).ifPresent(sprite -> {
            S entity = render_state.entity;
            render_state.getRenderer(sprite).addToRenderList(detail, render_state,
                    render_state.render_state.isResponding(entity));
        });
    }

    public final int getTriangleCount(@NonNull ElementRenderState<S> render_state, @NonNull PolyDetail detail) {
        return getSpriteKey(render_state)
                .map(sprite -> render_state.getRenderer(sprite).getTriangleCount(detail))
                .orElse(0);
    }

    public final float getEyeDistanceSquared(@NonNull ElementRenderState<S> render_state) {
        S entity = render_state.entity;
        CameraState camera = render_state.render_state.getCamera();
        return RenderTools.getEyeDistanceSquared(entity.bounds(), camera.getCurrentX(), camera.getCurrentY(), camera
                .getCurrentZ());
    }

    public abstract @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<S> render_state);

    public abstract void getTransform(@NonNull ElementRenderState<S> render_state, @NonNull Matrix4f dest);

    public abstract @NonNull Color getTeamColor(@NonNull ElementRenderState<S> render_state);

    public abstract @NonNull Color getSelectionColor(@NonNull ElementRenderState<S> render_state);

    public abstract Selectable.@NonNull VisualPattern getPattern(@NonNull ElementRenderState<S> render_state);
}
