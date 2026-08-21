package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.ModelClient;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;

/**
 * Logic for unit repairing behavior.
 * Manages units moving to damaged buildings and restoring their hit points.
 */
public final class RepairBehaviour implements Behaviour {
    private static final int REPAIRS_PER_SUPPLY = 5;
    private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;
    private final Building building;
    private final Unit unit;

    private float anim_time;
    private int repairs;
    private boolean sound;

    public RepairBehaviour(Unit unit, Building building) {
        this.unit = unit;
        this.building = building;
        unit.aimAtTarget(building);
        restartAnimation();
        unit.getSupplyContainer().increaseSupply(-1, SupplyType.WOOD);
        repairs = 0;
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
            unit.getClientState(ModelClient.class).ifPresent(ModelClient::onRepair);
        }

        if (anim_time > SECONDS_PER_ANIMATION_CYCLE) {
            restartAnimation();
            repairs++;
            if (building.isDead() || (building.isBuilt() && !building.isDamaged())) {
                return State.DONE;
            } else
                building.repair(1);
        }

        return repairs == REPAIRS_PER_SUPPLY ? State.DONE : State.INTERRUPTIBLE;
    }

    private void restartAnimation() {
        anim_time = 0;
        sound = false;
        unit.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
    }

    @Override
    public void forceInterrupted() {
    }
}
