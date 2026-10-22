package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;

/**
 * A base class for weapons that rotate as they are thrown (e.g., axes).
 */
public abstract sealed class RotatingThrowingWeapon extends ThrowingWeapon permits
        RockAxeWeapon, IronAxeWeapon, RubberAxeWeapon {
    private static final float OFFSET_X = 0.46f;
    private static final float OFFSET_Y = 0.02f;
    private static final float OFFSET_Z = 1.44f;

    public RotatingThrowingWeapon(boolean hit, Unit src, Selectable<?> target) {
        super(hit, src, target, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }
}
