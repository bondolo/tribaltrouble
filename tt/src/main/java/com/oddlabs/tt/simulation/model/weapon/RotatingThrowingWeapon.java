package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import org.jspecify.annotations.NonNull;

/**
 * A base class for weapons that rotate as they are thrown (e.g., axes).
 */
public abstract sealed class RotatingThrowingWeapon extends ThrowingWeapon permits
        RockAxeWeapon, IronAxeWeapon, RubberAxeWeapon {
    private float angle = 0;

    public RotatingThrowingWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        super(hit, src, target);
    }

    private void setAngle(float angle) {
        this.angle = angle;
    }

    public final float getAngle() {
        return angle;
    }

    @Override
    public final void animate(float t) {
        super.animate(t);
        setAngle(getAngle() + getAngleVelocity() * t);
    }

    protected abstract float getAngleVelocity();

    @Override
    protected float getLoftFactor() {
        return 1.01f;
    }
}
