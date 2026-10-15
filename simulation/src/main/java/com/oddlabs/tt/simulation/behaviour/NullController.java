package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Selectable;

/**
 * Controller providing no-op behaviour for stationary entities and buildings.
 */
public final class NullController extends Controller {
    private final Selectable<?> selectable;

    public NullController(Selectable<?> s) {
        super(0);
        this.selectable = s;
    }

    @Override
    public void decide() {
        selectable.setBehaviour(new NullBehaviour());
    }
}
