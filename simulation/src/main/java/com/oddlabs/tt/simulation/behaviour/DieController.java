package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Unit;

public final class DieController extends Controller {
    private final Unit unit;

    public DieController(Unit unit) {
        super(0);
        this.unit = unit;
    }

    @Override
    public void decide() {
        unit.setBehaviour(new DieBehaviour(unit));
    }
}
