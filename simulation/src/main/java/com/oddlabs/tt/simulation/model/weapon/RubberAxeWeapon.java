package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.AttackScanFilter;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponVisualType;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.NonNull;

/**
 * A throwing axe weapon made of rubber.
 */
public final class RubberAxeWeapon extends RotatingThrowingWeapon {
    private static final float ROTS_PER_SECOND = 9;
    private static final float ANGLE_DELTA = ROTS_PER_SECOND * 360f;
    private static final int MAX_BOUNDS_LENGTH = 3;
    private static final float METERS_PER_SECOND = 30; //multiplied by meters/second (in 2D)
    private static final float BOUNCING_METERS_PER_SECOND = 10; //multiplied by meters/second (in 2D)

    private boolean bouncing = false;

    public RubberAxeWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        super(hit, src, target);
    }

    @Override
    protected float getAngleVelocity() {
        return ANGLE_DELTA;
    }

    @Override
    protected void hitTarget(boolean hit, @NonNull Player owner, @NonNull Selectable<?> target) {
        if (hit)
            damageTarget(target);
        AttackScanFilter filter = new AttackScanFilter(owner, MAX_BOUNDS_LENGTH);
        owner.getWorld().getUnitGrid().scan(filter, target.getGridX(), target.getGridY());
        Selectable<?> s = filter.removeTarget();
        if (s != null && owner.getWorld().getRandom().nextFloat() > .5f) {
            bouncing = true;
            setTarget(s);
        } else
            super.hitTarget(hit, owner, target);
    }

    @Override
    public @NonNull WeaponVisualType getWeaponVisualType() {
        return WeaponVisualType.RUBBER;
    }

    @Override
    protected float getMetersPerSecond() {
        return bouncing ? BOUNCING_METERS_PER_SECOND : METERS_PER_SECOND;
    }

    @Override
    protected int getDamage() {
        return 2;
    }
}
