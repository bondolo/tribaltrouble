package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/**
 * A base class for weapons that are thrown in a specific direction (e.g., spears).
 */
public abstract sealed class DirectedThrowingWeapon extends ThrowingWeapon permits RockSpearWeapon, IronSpearWeapon,
        RubberSpearWeapon {
    public DirectedThrowingWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        super(hit, src, target);
    }

    @Override
    protected float getLoftFactor() {
        return 1.05f;
    }
}
