package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Unit;

/**
 * Controller that manages the application of the stun state to a unit.
 */
public final class StunController extends Controller {
    private final Unit unit;
    private final StunBehaviour stun_behaviour;

    private float time;

    public StunController(Unit unit, float time) {
        super(0);
        this.unit = unit;
        this.time = time;
        stun_behaviour = new StunBehaviour(this, unit);
    }

    public float getTime() {
        return time;
    }

    public boolean shouldSleep(float t) {
        time -= t;
        return time > 0;
    }

    @Override
    public void decide() {
        unit.setBehaviour(stun_behaviour);
        if (!shouldSleep(0f)) {
            unit.popController();
        }
    }
}
