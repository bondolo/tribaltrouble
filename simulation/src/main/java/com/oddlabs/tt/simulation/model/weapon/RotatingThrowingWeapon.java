package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;

/**
 * A base class for weapons that rotate as they are thrown (e.g., axes).
 */
public abstract sealed class RotatingThrowingWeapon extends ThrowingWeapon permits
        RockAxeWeapon, IronAxeWeapon, RubberAxeWeapon {
    public RotatingThrowingWeapon(boolean hit, Unit src, Selectable<?> target) {
        super(hit, src, target);
    }
}
