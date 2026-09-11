package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;

/**
 * A throwing axe weapon made of rock.
 */
public final class RockAxeWeapon extends RotatingThrowingWeapon {
    private static final float METERS_PER_SECOND = 20f; //multiplied by meters/second (in 2D)

    public RockAxeWeapon(boolean hit, Unit src, Selectable<?> target) {
        super(hit, src, target);
    }

    @Override
    public SupplyType getSupplyType() {
        return SupplyType.ROCK;
    }

    @Override
    public float getMetersPerSecond() {
        return METERS_PER_SECOND;
    }

    @Override
    protected int getDamage() {
        return 1;
    }
}
