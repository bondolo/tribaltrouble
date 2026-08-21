package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Tracks the specific rendering properties (position, color, pattern)
 * for an individual world element.
 */
public final class ElementSceneContext<M extends Model> implements ModelState<M> {

    public final SceneContext sceneContext;
    public ModelVisitor<M> visitor;
    public M model;
    public float f;
    private Color.Linear color = Color.Linear.WHITE;

    public ElementSceneContext(SceneContext sceneContext) {
        this.sceneContext = sceneContext;
    }

    @Override
    public Color.Linear getColor() {
        return color;
    }

    public void setColor(Color.Linear color) {
        this.color = color;
    }

    public void resetColor() {
        this.color = Color.Linear.WHITE;
    }

    @Override
    public @Nullable M getModel() {
        return model;
    }

    @Override
    public Matrix4f getTransform(Matrix4f dest) {
        visitor.getTransform(this, dest);
        return dest;
    }

    @Override
    public int getAnimation() {
        return model.getAnimation();
    }

    @Override
    public float getAnimationTicks() {
        return model.getAnimationTicks();
    }

    @Override
    public Color getTeamColor() {
        return visitor.getTeamColor(this);
    }

    @Override
    public Color getSelectionColor() {
        return visitor.getSelectionColor(this);
    }

    @Override
    public Selectable.VisualPattern getPattern() {
        return visitor.getPattern(this);
    }

    public void setup(ModelVisitor<M> visitor, M model, float f) {
        this.visitor = visitor;
        this.model = model;
        this.f = f;
        resetColor();
    }

    public void setup(ModelVisitor<M> visitor, M model) {
        this.visitor = visitor;
        this.model = model;
        resetColor();
    }

    @Override
    public void markDetailPoint() {
        visitor.markDetailPoint(this);
    }

    @Override
    public void markDetailPolygon(PolyDetail detail) {
        visitor.markDetailPolygon(this, detail);
    }

    @Override
    public int getTriangleCount(PolyDetail detail) {
        return visitor.getTriangleCount(this, detail);
    }

    @Override
    public float getEyeDistanceSquared() {
        return visitor.getEyeDistanceSquared(this);
    }

    public SpriteRenderer getRenderer(SpriteKey key) {
        return sceneContext.getRenderQueues().getRenderer(key);
    }
}
