package com.oddlabs.tt.effects.render;


import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.effects.particle.Particle;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class ParticleModelState implements ModelState<Particle> {
    private static final Color NO_SELECTION = Color.Linear.TRANSPARENT;
    private final Particle particle;
    private final Matrix4fc viewMatrix;

    public ParticleModelState(Particle particle, Matrix4fc viewMatrix) {
        this.particle = particle;
        this.viewMatrix = viewMatrix;
    }

    @Override
    public Particle getModel() {
        return particle;
    }

    @Override
    public int getAnimation() {
        return 0;
    }

    @Override
    public float getAnimationTicks() {
        return 0f;
    }

    @Override
    public Color getTeamColor() {
        return particle.getColor();
    }

    @Override
    public Color getSelectionColor() {
        return NO_SELECTION;
    }

    @Override
    public Selectable.VisualPattern getPattern() {
        return Selectable.VisualPattern.NONE;
    }

    @Override
    public Color getColor() {
        return particle.getColor();
    }

    @Override
    public Matrix4f getTransform(Matrix4f dest) {
        // Create the billboard transformation
        // 1. Translate to the particle's position
        dest.translation(particle.getPosX(), particle.getPosY(), particle.getPosZ());

        // 2. Apply the inverse of the camera's rotation to face the camera
        // We can copy the transposed upper 3x3 of the view matrix to achieve this
        dest.m00(viewMatrix.m00());
        dest.m01(viewMatrix.m10());
        dest.m02(viewMatrix.m20());
        dest.m10(viewMatrix.m01());
        dest.m11(viewMatrix.m11());
        dest.m12(viewMatrix.m21());
        dest.m20(viewMatrix.m02());
        dest.m21(viewMatrix.m12());
        dest.m22(viewMatrix.m22());

        // 3. Apply the particle's rotation angle around its forward facing axis
        dest.rotate(particle.getAngle(), 0f, 0f, 1f);

        // 4. Scale the particle
        dest.scale(particle.getRadiusX(), particle.getRadiusY(), particle.getRadiusZ());

        return dest;
    }

    @Override
    public float getEyeDistanceSquared() {
        return 0;
    }

    @Override
    public int getTriangleCount(PolyDetail detail) {
        return 2;
    }

    @Override
    public void markDetailPolygon(PolyDetail detail) {
        // No-op for particles
    }

    @Override
    public void markDetailPoint() {
        // No-op for particles
    }
}
