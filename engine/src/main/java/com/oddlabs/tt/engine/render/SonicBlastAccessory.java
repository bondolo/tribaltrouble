package com.oddlabs.tt.engine.render;

/**
 * Interface for accessories that can trigger sonic shockwave effects.
 */
public interface SonicBlastAccessory extends AnimatedAccessory {
    /**
     * Triggers a sonic blast shockwave effect at the specified target coordinates.
     *
     * @param targetX target x coordinate
     * @param targetY target y coordinate
     * @param targetZ target z coordinate
     * @param radius shockwave radius
     * @param duration shockwave duration
     */
    void triggerBlast(float targetX, float targetY, float targetZ, float radius, float duration);
}
