package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.weapon.Magic;
import com.oddlabs.tt.simulation.model.weapon.MagicFactory;
import org.jspecify.annotations.Nullable;

public final class MagicBehaviour implements Behaviour {
    private enum MagicState {
        PREPARING,
        CASTING,
        ENDING
    }

    private final Unit unit;
    private final MagicFactory magic_factory;
    private final MagicController controller;
    private @Nullable Magic magic;

    private float anim_time;
    private MagicState state = MagicState.PREPARING;

    public MagicBehaviour(Unit unit, MagicFactory magic_factory,
            MagicController controller) {
        this.unit = unit;
        this.magic_factory = magic_factory;
        this.controller = controller;
        anim_time = magic_factory.getSecondsPerInit();
        unit.switchAnimation(1f / magic_factory.getSecondsPerAnim(), Unit.Animation.MAGIC);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public State animate(float t) {
        anim_time -= t;
        return switch (state) {
            case PREPARING -> {
                if (anim_time <= 0) {
                    state = MagicState.CASTING;
                    magic = magic_factory.execute(unit);
                    anim_time += magic_factory.getSecondsPerRelease() - magic_factory.getSecondsPerInit();
                }
                yield State.UNINTERRUPTIBLE;
            }
            case CASTING -> {
                if (anim_time <= 0) {
                    state = MagicState.ENDING;
                    unit.getOwner().getWorld().getAnimationManagerGameTime().registerAnimation(magic);
                    anim_time += magic_factory.getSecondsPerAnim() - magic_factory.getSecondsPerRelease();
                }
                yield State.UNINTERRUPTIBLE;
            }
            case ENDING -> {
                if (anim_time > 0)
                    yield State.UNINTERRUPTIBLE;
                else {
                    controller.popNextTime();
                    yield State.DONE;
                }
            }
        };
    }

    @Override
    public void forceInterrupted() {
        if (magic != null)
            magic.interrupt();
    }
}
