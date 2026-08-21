package com.oddlabs.tt.simulation.model;

import com.oddlabs.util.Color;

/**
 * Interface for world objects that can cast a dynamic shadow decal.
 */
public interface Shadowable {
    /** diameter of the shadow or zero for no shadow */
    float getShadowDiameter();

    /** darkness of the shadow */
    default float getShadowOpacity() {
        return 0.5f;
    }

    /** vertical center of the shadow relative to object height */
    default float getShadowVerticalCenter() {
        return 0.6f;
    }

    default Color.Linear getShadowColor() {
        return Color.Linear.BLACK;
    }

    default float getShadowPattern() {
        return 0.0f;
    }

    float getPositionX();

    float getPositionY();

    default boolean isDead() {
        return false;
    }
}
