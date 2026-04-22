package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Accessory;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/**
 * Behaviour that plays the idle animation and handles the stun duration.
 */
public final class StunBehaviour implements Behaviour {
    private final @NonNull StunController controller;
    private final @NonNull Unit unit;
    private final @NonNull Accessory accessory;

    public StunBehaviour(@NonNull StunController controller, @NonNull Unit unit, @NonNull Accessory accessory) {
        this.controller = controller;
        this.unit = unit;
        this.accessory = accessory;
        unit.addAccessory(accessory);
    }

    @Override
    public @NonNull State animate(float t) {
        unit.switchToIdleAnimation();
        return !controller.shouldSleep(t) ? State.DONE : State.UNINTERRUPTIBLE;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void forceInterrupted() {
    }

    @Override
    public void onCleanup() {
        unit.removeAccessory(accessory);
    }
}
