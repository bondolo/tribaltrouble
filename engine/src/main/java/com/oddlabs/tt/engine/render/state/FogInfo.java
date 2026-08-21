package com.oddlabs.tt.engine.render.state;

import com.oddlabs.util.Color;

/**
 * Base configuration for distance and screen-space fog rendering.
 */
public class FogInfo {

    public enum Mode {
        /**
         * No fog should be applied.
         */
        NONE,
        /**
         * Standard distance-based fog (linear, exp, exp2).
         */
        LINEAR,
        EXP,
        EXP2,
        /**
         * Screen-space radial fog for map view.
         */
        RADIAL
    }

    protected final Mode mode;
    protected final Color.Linear color;
    protected final float density;
    private boolean enabled = true;

    public FogInfo(Mode mode, Color color, float density) {
        this.mode = mode;
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
        this.density = density;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the base color of the fog.
     *
     * @return Linear color
     */
    public Color.Linear getColor() {
        return color;
    }

    public float getDensity() {
        return density;
    }

    public Mode getMode() {
        return mode;
    }
}
