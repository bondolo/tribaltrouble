package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponVisualType;

/**
 * A throwing axe weapon made of rock.
 */
public final class RockAxeWeapon extends RotatingThrowingWeapon {
    private static final float ROTS_PER_SECOND = 3;
    private static final float ANGLE_DELTA = ROTS_PER_SECOND * 360f;
    private static final float METERS_PER_SECOND = 20f; //multiplied by meters/second (in 2D)

    public RockAxeWeapon(boolean hit, Unit src, Selectable<?> target) {
        super(hit, src, target);
    }

    @Override
    protected float getAngleVelocity() {
        return ANGLE_DELTA;
    }

    @Override
    public WeaponVisualType getWeaponVisualType() {
        return WeaponVisualType.ROCK;
    }

    @Override
    protected float getMetersPerSecond() {
        return METERS_PER_SECOND;
    }

    @Override
    protected int getDamage() {
        return 1;
    }
}
