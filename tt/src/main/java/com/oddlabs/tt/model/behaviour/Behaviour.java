package com.oddlabs.tt.model.behaviour;

import org.jspecify.annotations.NonNull;

public sealed interface Behaviour permits AttackBehaviour, DieBehaviour, HarvestBehaviour, IdleBehaviour, MagicBehaviour, NullBehaviour, RepairBehaviour, StunBehaviour, WalkBehaviour {
    enum State {
        UNINTERRUPTIBLE, INTERRUPTIBLE, DONE
    }

    @NonNull State animate(float t);

    boolean isBlocking();

    void forceInterrupted();
}
