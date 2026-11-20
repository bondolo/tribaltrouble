package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.weapon.Magic;
import com.oddlabs.tt.simulation.model.weapon.MagicFactory;
import org.jspecify.annotations.Nullable;

/// Uninterruptible spell-casting state managing preparation, execution, registration, and post-cast recovery.
public final class CastingMagic implements UnitState {
    private enum Phase {
        PREPARING,
        CASTING,
        ENDING
    }

    private final MagicFactory magicFactory;
    private final UnitState fallbackState;
    private @Nullable Magic magic;
    private float animTime;
    private Phase phase = Phase.PREPARING;
    private boolean shouldPop;

    public CastingMagic(MagicFactory magicFactory, UnitState fallbackState) {
        this.magicFactory = magicFactory;
        this.fallbackState = fallbackState;
    }

    @Override
    public Transition decide(UnitContext context) {
        if (shouldPop) {
            return Transition.to(fallbackState);
        }
        phase = Phase.PREPARING;
        magic = null;
        animTime = magicFactory.getSecondsPerInit();
        context.switchAnimation(1f / magicFactory.getSecondsPerAnim(), Unit.Animation.MAGIC);
        return Transition.stay();
    }

    @Override
    public TickResult tick(float dt, UnitContext context) {
        animTime -= dt;
        return switch (phase) {
            case PREPARING -> {
                if (animTime <= 0) {
                    phase = Phase.CASTING;
                    magic = context.executeMagic(magicFactory);
                    animTime += magicFactory.getSecondsPerRelease() - magicFactory.getSecondsPerInit();
                }
                yield TickResult.UNINTERRUPTIBLE;
            }
            case CASTING -> {
                if (animTime <= 0) {
                    phase = Phase.ENDING;
                    if (magic != null) {
                        context.registerMagic(magic);
                    }
                    animTime += magicFactory.getSecondsPerAnim() - magicFactory.getSecondsPerRelease();
                }
                yield TickResult.UNINTERRUPTIBLE;
            }
            case ENDING -> {
                if (animTime > 0) {
                    yield TickResult.UNINTERRUPTIBLE;
                } else {
                    shouldPop = true;
                    yield TickResult.DONE;
                }
            }
        };
    }

    void interrupt() {
        if (magic != null) {
            magic.interrupt();
        }
    }

    @Override
    public UnitState getFallbackState() {
        return fallbackState;
    }
}
