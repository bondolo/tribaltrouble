package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.WeaponVisualType;
import org.jspecify.annotations.NonNull;

/**
 * A throwing spear weapon made of rock.
 */
public final class RockSpearWeapon extends DirectedThrowingWeapon {
    private static final float METERS_PER_SECOND = 20f; //multiplied by meters/second (in 2D)

    public RockSpearWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        super(hit, src, target);
    }

    @Override
    public @NonNull WeaponVisualType getWeaponVisualType() {
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
