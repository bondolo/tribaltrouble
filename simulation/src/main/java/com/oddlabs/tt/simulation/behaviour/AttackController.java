package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;

/** AttackController attacks a target until it is dead or the unit is no longer close enough to attack. */
public final class AttackController extends Controller {

    private final Selectable<?> target;
    private final Unit unit;

    public AttackController(Unit unit, Selectable<?> target) {
        super(0);
        this.unit = unit;
        this.target = target;
    }

    private boolean canAttack() {
        return unit.isCloseEnough(unit.getRange(target), target);
    }

    @Override
    public void decide() {
        if (target.isDead() || !canAttack()) {
            unit.popController();
        } else {
            unit.setBehaviour(new AttackBehaviour(unit, target));
        }
    }
}
