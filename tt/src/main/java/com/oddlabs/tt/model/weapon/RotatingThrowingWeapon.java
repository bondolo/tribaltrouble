package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/**
 * A base class for weapons that rotate as they are thrown (e.g., axes).
 */
public abstract sealed class RotatingThrowingWeapon extends ThrowingWeapon permits
        RockAxeWeapon, IronAxeWeapon, RubberAxeWeapon {
    public RotatingThrowingWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        super(hit, src, target);
    }

    @Override
    public boolean isRotating() {
        return true;
    }

    @Override
    protected float getLoftFactor() {
        return 1.01f;
    }
}
