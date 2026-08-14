package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponVisualType;
import org.jspecify.annotations.NonNull;

/**
 * A throwing axe weapon made of iron.
 */
public final class IronAxeWeapon extends RotatingThrowingWeapon {
    private static final float ROTS_PER_SECOND = 6;
    private static final float ANGLE_DELTA = ROTS_PER_SECOND * 360f;
    private static final float METERS_PER_SECOND = 25f; //multiplied by meters/second (in 2D)

    public IronAxeWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        super(hit, src, target);
    }

    @Override
    public @NonNull WeaponVisualType getWeaponVisualType() {
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
