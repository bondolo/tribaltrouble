package com.oddlabs.tt.render;

import com.oddlabs.tt.model.AccessorizableModel;
import com.oddlabs.tt.model.Accessory;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Specialized render state for handling attached accessories.
 */
final class AttachedRenderState implements ModelState<Model> {
    private @NonNull ElementRenderState<?> parentState;
    private @NonNull Accessory accessory;

    AttachedRenderState() {
    }

    void setup(@NonNull ElementRenderState<?> parentState, @NonNull Accessory accessory) {
        this.parentState = parentState;
        this.accessory = accessory;
    }

    @Override
    public @NonNull Matrix4f getTransform(@NonNull Matrix4f dest) {
        parentState.getTransform(dest);
        accessory.getRelativeTransform(dest, (AccessorizableModel) parentState.model);
        return dest;
    }

    @Override
    public @NonNull Vector4fc getTeamColor() {
        return parentState.getTeamColor();
    }

    @Override
    public @NonNull Vector4fc getSelectionColor() {
        return parentState.getSelectionColor();
    }

    @Override
    public @NonNull Vector4fc getColor() {
        return parentState.getColor();
    }

    @Override
    public Selectable.@NonNull VisualPattern getPattern() {
        return parentState.getPattern();
    }

    @Override
    public @Nullable Model getModel() {
        return parentState.model;
    }

    @Override
    public void markDetailPoint() {
        // Accessories typically don't have their own detail markers in the current model
    }

    @Override
    public void markDetailPolygon(@NonNull PolyDetail detail) {
        // Standard sprite rendering
        SpriteKey key = accessory.getSpriteRenderer();
        if (key != null) {
            parentState.render_state.getRenderQueues().getRenderer(key).addToRenderList(detail, this, parentState.render_state.isResponding(parentState.model));
        }
    }

    @Override
    public int getTriangleCount(@NonNull PolyDetail detail) {
        SpriteKey key = accessory.getSpriteRenderer();
        if (key != null) {
            return parentState.render_state.getRenderQueues().getRenderer(key).getTriangleCount(detail);
        }
        return 0;
    }

    @Override
    public float getEyeDistanceSquared() {
        return parentState.getEyeDistanceSquared();
    }
}
