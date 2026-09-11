package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;

/**
 * A base class for weapons that are thrown in a specific direction (e.g., spears).
 */
public abstract sealed class DirectedThrowingWeapon extends ThrowingWeapon permits RockSpearWeapon, IronSpearWeapon,
        RubberSpearWeapon {
    private static final float OFFSET_X = 1.25f;
    private static final float OFFSET_Y = -0.35f;
    private static final float OFFSET_Z = 1.78f;

    public DirectedThrowingWeapon(boolean hit, Unit src, Selectable<?> target) {
        super(hit, src, target, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }
}
