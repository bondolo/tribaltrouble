package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import org.jspecify.annotations.Nullable;

/// Harvesting state locating a supply resource, walking into range, and executing swing cycles until full or depleted.
public final class Harvesting implements UnitState {
    private final SupplyType supplyType;
    private final UnitState fallbackState;
    private final GiveUpTracker.Single giveUpTracker = new GiveUpTracker.Single();
    private final HarvestCyclePhase harvestPhase = new HarvestCyclePhase();
    private @Nullable Supply supply;
    private boolean trackerActive;

    public Harvesting(@Nullable Supply supply, SupplyType supplyType, UnitState fallbackState) {
        this.supply = supply;
        this.supplyType = supplyType;
        this.fallbackState = fallbackState;
    }

    @Override
    public Transition decide(UnitContext context) {
        harvestPhase.reset();
        if (context.getCarriedSupplyType() == supplyType && context.isSupplyFull()) {
            return Transition.to(fallbackState);
        }
        if (trackerActive) {
            supply = context.getFoundSupply();
        }
        if (supply != null && !supply.isEmpty() && context.isCloseEnough(0f, supply)) {
            giveUpTracker.reset();
            harvestPhase.start(supply, context);
            return Transition.stay();
        }
        if (!giveUpTracker.shouldGiveUp()) {
            trackerActive = true;
            context.initSupplyFinderWalk(supplyType);
            return Transition.walk(false);
        }
        return Transition.to(fallbackState);
    }

    @Override
    public TickResult tick(float dt, UnitContext context) {
        return harvestPhase.isActive() ? harvestPhase.tick(dt, context) : TickResult.DONE;
    }

    boolean isHarvestActive() {
        return harvestPhase.isActive();
    }

    @Override
    public UnitState getFallbackState() {
        return fallbackState;
    }

    @Override
    public void resetGiveUpCounters() {
        giveUpTracker.reset();
    }

    /// Reusable 1-second harvesting swing cycle striking a supply node at the weapon release point.
    private static final class HarvestCyclePhase {
        private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;

        private @Nullable Supply supply;
        private float animTime;
        private boolean sound;

        void reset() {
            this.supply = null;
        }

        void start(Supply supply, UnitContext context) {
            this.supply = supply;
            context.aimAtTarget(supply);
            restartAnimation(context);
        }

        boolean isActive() {
            return supply != null;
        }

        private void restartAnimation(UnitContext context) {
            context.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
            animTime = 0f;
            sound = false;
        }

        TickResult tick(float dt, UnitContext context) {
            Supply activeSupply = supply;
            if (activeSupply == null) {
                return TickResult.DONE;
            }
            animTime += dt;
            if (animTime > context.getWeaponSecondsPerRelease(1f / SECONDS_PER_ANIMATION_CYCLE) && !sound) {
                sound = true;
                context.onHarvestSwing(activeSupply);
            }
            if (animTime > SECONDS_PER_ANIMATION_CYCLE) {
                restartAnimation(context);
                if (context.isSupplyFull() || activeSupply.isEmpty()) {
                    return TickResult.DONE;
                }
            }
            return TickResult.INTERRUPTIBLE;
        }
    }
}
