package com.oddlabs.tt.effects.particle;

import com.oddlabs.util.Color;
import org.joml.Vector3f;

/**
 * Base data representation for an individual visual particle.
 */
public class Particle {
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
    }

    public final float getAngle() {
        return angle;
    }

    final void setAngle(float angle) {
        this.angle = angle;
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

    final void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public final float getAngularVelocity() {
        return angularVelocity;
    }

    final void setPos(float x, float y, float z) {
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

    /**
     * Sets the particle colour components in linear RGB space.
     *
     * @param r linear red channel
     * @param g linear green channel
     * @param b linear blue channel
     * @param a alpha channel
     */
    final void setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
    }

    public final void addColor(float r, float g, float b, float a) {
        this.colorR += r;
        this.colorG += g;
        this.colorB += b;
        this.colorA += a;
    }

    public final void addColor(Color.LinearDelta delta) {
        addColor(delta.r(), delta.g(), delta.b(), delta.a());
    }

    /**
     * Resolves the particle color in linear RGB space as a {@link Color.Linear} object,
     * reflecting any subclass component modulation.
     *
     * @return current linear RGB color
     */
    public final Color.Linear getColor() {
        return new Color.Linear(getColorR(), getColorG(), getColorB(), getColorA());
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

    /**
     * Sets the per-second rate of color change in linear RGB space.
     *
     * @param r linear red delta per second
     * @param g linear green delta per second
     * @param b linear blue delta per second
     * @param a alpha delta per second
     */
    final void setDeltaColor(float r, float g, float b, float a) {
        this.deltaR = r;
        this.deltaG = g;
        this.deltaB = b;
        this.deltaA = a;
    }

    final void setEnergy(float energy) {
        this.energy = energy;
    }

    public final float getEnergy() {
        return energy;
    }

    public final boolean isDead() {
        return energy <= 0f;
    }

    final void setType(int type) {
        this.type = type;
    }

    public final int getType() {
        return type;
    }

    final void setGrowthRate(float growth_rate_x, float growth_rate_y, float growth_rate_z) {
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

    final void setRadius(float radius_x, float radius_y, float radius_z) {
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
