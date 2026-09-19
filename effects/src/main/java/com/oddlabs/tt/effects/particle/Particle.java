package com.oddlabs.tt.effects.particle;

import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Base data representation for an individual visual particle.
 */
public class Particle {
    private final float u1;
    private final float v1;
    private final float u2;
    private final float v2;
    private final float u3;
    private final float v3;
    private final float u4;
    private final float v4;
    private float angle;
    private float angularVelocity = 0f;

    private final Vector3f position = new Vector3f();
    private float colorR = 0f;
    private float colorG = 0f;
    private float colorB = 0f;
    private float colorA = 0f;
    private float deltaR = 0f;
    private float deltaG = 0f;
    private float deltaB = 0f;
    private float deltaA = 0f;
    private final Vector3f growthRate = new Vector3f();
    private final Vector3f radius = new Vector3f();

    private int type;
    private float energy;

    public Particle() {
        this(0f);
    }

    public Particle(float angle) {
        this.angle = angle;
        Matrix4f rotMatrix = new Matrix4f();
        Vector3f axis = new Vector3f(0f, 0f, 1f);
        Vector4f uvVector = new Vector4f();

        rotMatrix.rotate(angle, axis);

        uvVector.set(-.5f, -.5f, 0f, 0f);
        rotMatrix.transform(uvVector);
        u1 = uvVector.x() + .5f;
        v1 = .5f - uvVector.y();

        uvVector.set(.5f, -.5f, 0f, 0f);
        rotMatrix.transform(uvVector);
        u2 = uvVector.x() + .5f;
        v2 = .5f - uvVector.y();

        uvVector.set(.5f, .5f, 0f, 0f);
        rotMatrix.transform(uvVector);
        u3 = uvVector.x() + .5f;
        v3 = .5f - uvVector.y();

        uvVector.set(-.5f, .5f, 0f, 0f);
        rotMatrix.transform(uvVector);
        u4 = uvVector.x() + .5f;
        v4 = .5f - uvVector.y();
    }

    public final float getU1() {
        return u1;
    }

    public final float getV1() {
        return v1;
    }

    public final float getU2() {
        return u2;
    }

    public final float getV2() {
        return v2;
    }

    public final float getU3() {
        return u3;
    }

    public final float getV3() {
        return v3;
    }

    public final float getU4() {
        return u4;
    }

    public final float getV4() {
        return v4;
    }

    public final float getAngle() {
        return angle;
    }

    public void update(float t) {
        colorR += deltaR * t;
        colorG += deltaG * t;
        colorB += deltaB * t;
        colorA += deltaA * t;
        radius.add(growthRate.x() * t, growthRate.y() * t, growthRate.z() * t);
        angle += angularVelocity * t;
        energy -= t;
    }

    public final void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public final float getAngularVelocity() {
        return angularVelocity;
    }

    public final void setPos(float x, float y, float z) {
        position.set(x, y, z);
    }

    public final float getPosX() {
        return position.x();
    }

    public final float getPosY() {
        return position.y();
    }

    public final float getPosZ() {
        return position.z();
    }

    final void setColor(Color.Linear color) {
        this.colorR = color.r();
        this.colorG = color.g();
        this.colorB = color.b();
        this.colorA = color.a();
    }

    public final void addColor(Color.LinearDelta delta) {
        this.colorR += delta.r();
        this.colorG += delta.g();
        this.colorB += delta.b();
        this.colorA += delta.a();
    }

    public final Color.Linear getColor() {
        return new Color.Linear(colorR, colorG, colorB, colorA);
    }

    public float getColorR() {
        return colorR;
    }

    public float getColorG() {
        return colorG;
    }

    public float getColorB() {
        return colorB;
    }

    public final float getColorA() {
        return colorA;
    }

    public final Color.LinearDelta getDeltaColor() {
        return new Color.LinearDelta(deltaR, deltaG, deltaB, deltaA);
    }

    public final void setDeltaColor(Color.LinearDelta delta) {
        this.deltaR = delta.r();
        this.deltaG = delta.g();
        this.deltaB = delta.b();
        this.deltaA = delta.a();
    }

    public final void setEnergy(float energy) {
        this.energy = energy;
    }

    public final float getEnergy() {
        return energy;
    }

    public final boolean isDead() {
        return energy <= 0f;
    }

    public final void setType(int type) {
        this.type = type;
    }

    public final int getType() {
        return type;
    }

    public final void setGrowthRate(float growth_rate_x, float growth_rate_y, float growth_rate_z) {
        this.growthRate.set(growth_rate_x, growth_rate_y, growth_rate_z);
    }

    public final float getGrowthRateX() {
        return growthRate.x();
    }

    public final float getGrowthRateY() {
        return growthRate.y();
    }

    public final float getGrowthRateZ() {
        return growthRate.z();
    }

    public final void setRadius(float radius_x, float radius_y, float radius_z) {
        this.radius.set(radius_x, radius_y, radius_z);
    }

    public final float getRadiusX() {
        return radius.x();
    }

    public final float getRadiusY() {
        return radius.y();
    }

    public final float getRadiusZ() {
        return radius.z();
    }
}
