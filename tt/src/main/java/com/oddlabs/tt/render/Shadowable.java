package com.oddlabs.tt.render;

/**
 * Interface for world objects that can cast a dynamic shadow decal.
 */
public interface Shadowable {
    /** diameter of the shadow */
    float getShadowDiameter();

    /** dankness of the shadow */
    default float getShadowOpacity() { return 0.5f; }

    /** vertical center of the shadow relative to object height */
    default float getShadowVerticalCenter() { return 0.6f; }

    float getPositionX();

    float getPositionY();

    default boolean isDead() { return false; }
}
