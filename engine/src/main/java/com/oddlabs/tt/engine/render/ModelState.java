package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Unified interface for accessing the visual state of renderable world objects.
 */
public interface ModelState<M extends Model> extends LODObject {
    Matrix4f getTransform(Matrix4f dest);

    int getAnimation();

    float getAnimationTicks();

    Color getTeamColor();

    Color getSelectionColor();

    Color getColor();

    Selectable.VisualPattern getPattern();

    @Nullable
    M getModel();
}
