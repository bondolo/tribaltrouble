package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Target;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.tt.model.snapshot.VisualSnapshots;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Tracks the specific rendering properties (position, color, pattern)
 * for an individual world element.
 */
final class ElementRenderState<S extends EntitySnapshot> implements ModelState<S> {

    final @NonNull RenderState render_state;
    private ModelVisitor<S> visitor;
    S entity;
    float f;
    private Color.@NonNull Linear color = Color.Linear.WHITE;

    ElementRenderState(@NonNull RenderState render_state) {
        this.render_state = render_state;
    }

    @Override
    public Color.@NonNull Linear getColor() {
        return color;
    }

    public void setColor(Color.@NonNull Linear color) {
        this.color = color;
    }

    public void resetColor() {
        this.color = Color.Linear.WHITE;
    }

    @Override
    public @NonNull S getEntity() {
        return entity;
    }

    @Override
    public float getNoDetailSize() {
        if (entity instanceof VisualSnapshots.UnitSnapshot unit) {
            return VisualRegistry.getInstance().getUnitVisuals(unit.race(), unit.visualType()).noDetailSize();
        } else if (entity instanceof VisualSnapshots.BuildingSnapshot building) {
            return VisualRegistry.getInstance().getBuildingVisuals(building.race(), building.buildingType())
                    .noDetailSize();
        }
        return 0f;
    }

    @Override
    public @Nullable Target getTarget() {
        var world = render_state.getLocalPlayer().getWorld();
        return world.getTargetById(entity.id());
    }

    @Override
    public @NonNull Matrix4f getTransform(@NonNull Matrix4f dest) {
        visitor.getTransform(this, dest);
        return dest;
    }

    @Override
    public int getAnimation() {
        return entity.animation();
    }

    @Override
    public float getAnimationTicks() {
        return entity.animationTicks();
    }

    @Override
    public @NonNull Color getTeamColor() {
        return visitor.getTeamColor(this);
    }

    @Override
    public @NonNull Color getSelectionColor() {
        return visitor.getSelectionColor(this);
    }

    @Override
    public Selectable.@NonNull VisualPattern getPattern() {
        return visitor.getPattern(this);
    }

    void setup(@NonNull ModelVisitor<S> visitor, @NonNull S entity, float f) {
        this.visitor = visitor;
        this.entity = entity;
        this.f = f;
        resetColor();
    }

    void setup(@NonNull ModelVisitor<S> visitor, @NonNull S entity) {
        this.visitor = visitor;
        this.entity = entity;
        resetColor();
    }

    @Override
    public void markDetailPoint() {
        visitor.markDetailPoint(this);
    }

    @Override
    public void markDetailPolygon(@NonNull PolyDetail detail) {
        visitor.markDetailPolygon(this, detail);
    }

    @Override
    public int getTriangleCount(@NonNull PolyDetail detail) {
        return visitor.getTriangleCount(this, detail);
    }

    @Override
    public float getEyeDistanceSquared() {
        return visitor.getEyeDistanceSquared(this);
    }

    @NonNull
    SpriteRenderer getRenderer(@NonNull SpriteKey key) {
        return render_state.getRenderQueues().getRenderer(key);
    }
}
