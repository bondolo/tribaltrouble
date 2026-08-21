package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;

public final class AttackBehaviour implements Behaviour {
    private static final float SECONDS_PER_ATTACK = 2f;

    enum AttackState {
        THROWING,
        RELEASED
    }

    private final Selectable<?> target;
    private final Unit unit;
    private float anim_time;
    private AttackState state = AttackState.THROWING;

    public AttackBehaviour(Unit unit, Selectable<?> target) {
        this.unit = unit;
        this.target = target;
        anim_time = unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ATTACK);
        unit.switchAnimation(1f / SECONDS_PER_ATTACK, Unit.Animation.THROWING);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public State animate(float t) {
        return switch (state) {
            case THROWING -> {
                updateAttack(t);
                if (anim_time <= 0) {
                    if (unit.isMounted())
                        unit.getWeaponFactory().attack(unit, target, 3f);
                    else
                        unit.getWeaponFactory().attack(unit, target);

                    anim_time += SECONDS_PER_ATTACK - unit.getWeaponFactory().getSecondsPerRelease(1f
                            / SECONDS_PER_ATTACK);
                    state = AttackState.RELEASED;
                }
                yield State.UNINTERRUPTIBLE;
            }
            case RELEASED -> {
                updateAttack(t);
                yield anim_time > 0 ? State.UNINTERRUPTIBLE : State.DONE;
            }
        };
    }

    private void updateAttack(float t) {
        anim_time -= t;
        unit.aimAtTarget(target);
    }

    @Override
    public void forceInterrupted() {
    }
}
