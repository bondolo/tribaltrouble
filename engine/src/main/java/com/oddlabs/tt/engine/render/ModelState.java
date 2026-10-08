package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.simulation.model.Model;
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

    default float getTeamColorR() {
        return getTeamColor().r();
    }

    default float getTeamColorG() {
        return getTeamColor().g();
    }

    default float getTeamColorB() {
        return getTeamColor().b();
    }

    default float getTeamColorA() {
        return getTeamColor().a();
    }

    Color getSelectionColor();

    Color getColor();

    default float getColorR() {
        return getColor().r();
    }

    default float getColorG() {
        return getColor().g();
    }

    default float getColorB() {
        return getColor().b();
    }

    default float getColorA() {
        return getColor().a();
    }

    VisualPattern getPattern();

    float getNoDetailSize();

    @Nullable
    M getModel();

    default @Nullable SpriteList getSpriteList() {
        return null;
    }
}
