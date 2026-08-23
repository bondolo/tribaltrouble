package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Specialized render state for handling attached accessories.
 */
public final class AttachedRenderState implements ModelState<Model> {
    private @Nullable ElementSceneContext<?> parentState;
    private @Nullable Accessory accessory;

    public AttachedRenderState() {
    }

    public void setup(ElementSceneContext<?> parentState, Accessory accessory) {
        this.parentState = parentState;
        this.accessory = accessory;
    }

    @Override
    public Matrix4f getTransform(Matrix4f dest) {
        assert parentState != null;
        assert accessory != null;

        parentState.getTransform(dest);
        accessory.getRelativeTransform(dest, parentState.model);

        if (accessory instanceof BillboardAccessory) {
            CameraState camera = parentState.sceneContext.getCamera();
            if (camera != null) {
                float tx = dest.m30();
                float ty = dest.m31();
                float tz = dest.m32();

                float sx = (float) Math.sqrt(dest.m00() * dest.m00() + dest.m01() * dest.m01() + dest.m02() * dest
                        .m02());
                float sy = (float) Math.sqrt(dest.m10() * dest.m10() + dest.m11() * dest.m11() + dest.m12() * dest
                        .m12());
                float sz = (float) Math.sqrt(dest.m20() * dest.m20() + dest.m21() * dest.m21() + dest.m22() * dest
                        .m22());

                dest.translation(tx, ty, tz);

                // Align billboard with camera (inverse of camera's model-view rotation)
                dest.m00(camera.getModelView().m00());
                dest.m01(camera.getModelView().m10());
                dest.m02(camera.getModelView().m20());
                dest.m10(camera.getModelView().m01());
                dest.m11(camera.getModelView().m11());
                dest.m12(camera.getModelView().m21());
                dest.m20(camera.getModelView().m02());
                dest.m21(camera.getModelView().m12());
                dest.m22(camera.getModelView().m22());

                dest.scale(sx, sy, sz);
            }
        }
        return dest;
    }

    @Override
    public Color getTeamColor() {
        assert parentState != null;
        return parentState.getTeamColor();
    }

    @Override
    public Color getSelectionColor() {
        assert parentState != null;
        return parentState.getSelectionColor();
    }

    @Override
    public Color getColor() {
        assert parentState != null;
        Color.Linear parentColor = parentState.getColor();
        if (accessory instanceof AlphaAccessory alphaAccessory) {
            float alpha = alphaAccessory.getAlpha();
            if (alpha < 1.0f) {
                return parentColor.alpha(parentColor.a() * alpha);
            }
        }
        return parentColor;
    }

    @Override
    public Selectable.VisualPattern getPattern() {
        assert parentState != null;
        return parentState.getPattern();
    }

    @Override
    public @Nullable Model getModel() {
        assert parentState != null;
        return parentState.model;
    }

    @Override
    public void markDetailPoint() {
        // Accessories typically don't have their own detail markers in the current model
    }

    @Override
    public void markDetailPolygon(PolyDetail detail) {
        assert accessory != null;
        assert parentState != null;

        // Standard sprite rendering
        SpriteKey key = accessory.getSpriteRenderer();
        if (key != null) {
            parentState.sceneContext.getRenderQueues().getRenderer(key).addToRenderList(detail, this,
                    parentState.sceneContext.isResponding(parentState.model));
        }
    }

    @Override
    public int getTriangleCount(PolyDetail detail) {
        assert accessory != null;
        assert parentState != null;

        SpriteKey key = accessory.getSpriteRenderer();
        if (key != null) {
            return parentState.sceneContext.getRenderQueues().getRenderer(key).getTriangleCount(detail);
        }
        return 0;
    }

    @Override
    public int getAnimation() {
        assert accessory != null;

        return accessory.getAnimation();
    }

    @Override
    public float getAnimationTicks() {
        assert accessory != null;
        assert parentState != null;

        return accessory.getAnimationTicks(parentState.model);
    }

    @Override
    public float getEyeDistanceSquared() {
        assert accessory != null;
        assert parentState != null;

        return parentState.getEyeDistanceSquared();
    }
}
