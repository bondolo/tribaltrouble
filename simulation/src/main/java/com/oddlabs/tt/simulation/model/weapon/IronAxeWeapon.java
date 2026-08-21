package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponVisualType;

/**
 * A throwing axe weapon made of iron.
 */
public final class IronAxeWeapon extends RotatingThrowingWeapon {
    private static final float ROTS_PER_SECOND = 6;
    private static final float ANGLE_DELTA = ROTS_PER_SECOND * 360f;
    private static final float METERS_PER_SECOND = 25f; //multiplied by meters/second (in 2D)

    public IronAxeWeapon(boolean hit, Unit src, Selectable<?> target) {
        super(hit, src, target);
    }

    @Override
    public WeaponVisualType getWeaponVisualType() {
        return WeaponVisualType.IRON;
    }

    @Override
    protected float getAngleVelocity() {
        return ANGLE_DELTA;
    }

    @Override
    protected float getMetersPerSecond() {
        return METERS_PER_SECOND;
    }

    @Override
    protected int getDamage() {
        return 2;
    }
}
