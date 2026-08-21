package com.oddlabs.tt.simulation.trigger;

import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.behaviour.MagicController;

/**
 * Trigger that executes a callback when a specific magic spell is cast by a chieftain.
 */
public final class MagicUsedTrigger extends IntervalTrigger {
    private final Unit chieftain;
    private final float x;
    private final float y;
    private final float r;
    private final MagicType magicType;
    private final Runnable runnable;

    private boolean blowing = false;

    public MagicUsedTrigger(Unit chieftain, float x, float y, float r, MagicType magicType,
            Runnable runnable) {
        super(chieftain.getOwner().getWorld(), 0f, 0f);
        this.chieftain = chieftain;
        this.x = x;
        this.y = y;
        this.r = r;
        this.magicType = magicType;
        this.runnable = runnable;
    }

    @Override
    protected void check() {
        float dx = chieftain.getPositionX() - x;
        float dy = chieftain.getPositionY() - y;
        if (!chieftain.isDead()) {
            if (r * r > dx * dx + dy * dy) {
                if (!blowing && chieftain.getPrimaryController() instanceof MagicController &&
                        chieftain.getLastMagicType() == magicType)
                    blowing = true;
                if (blowing && !(chieftain.getPrimaryController() instanceof MagicController))
                    triggered();
            }
        }
    }

    @Override
    protected void done() {
        runnable.run();
    }
}
