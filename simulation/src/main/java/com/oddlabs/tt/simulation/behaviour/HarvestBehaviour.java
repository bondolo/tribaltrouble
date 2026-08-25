package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.Unit;

/**
 * Logic for unit harvesting behavior.
 * Manages finding harvestable resources, moving to them, and returning resources to a collection point.
 */
public final class HarvestBehaviour implements Behaviour {
    private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;
    private final Supply supply;
    private final Unit unit;
    private float anim_time;
    private boolean sound;

    public HarvestBehaviour(Unit unit, Supply supply) {
        this.unit = unit;
        this.supply = supply;
        unit.aimAtTarget(supply);
        restartAnimation();
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public State animate(float t) {
        anim_time += t;
        if (anim_time > unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ANIMATION_CYCLE) && !sound) {
            sound = true;
            unit.getWorld().getNotificationListener().onHarvest(supply.getSupplyType(), unit.getPositionX(), unit
                    .getPositionY(), unit.getPositionZ());

            if (supply.hit()) {
                unit.getSupplyContainer().increaseSupply(1, supply.getSupplyType());
                unit.getOwner().harvested(supply.getSupplyType());
            }
        }

        if (anim_time > SECONDS_PER_ANIMATION_CYCLE) {
            restartAnimation();
            if (unit.getSupplyContainer().isSupplyFull() || supply.isEmpty())
                return State.DONE;
        }

        return State.INTERRUPTIBLE;
    }

    private void restartAnimation() {
        unit.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
        anim_time = 0;
        sound = false;
    }

    @Override
    public void forceInterrupted() {
    }
}
