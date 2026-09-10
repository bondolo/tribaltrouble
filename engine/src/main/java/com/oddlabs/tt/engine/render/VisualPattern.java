package com.oddlabs.tt.engine.render;

import com.oddlabs.util.Color;

/**
 * Visual selection and alignment patterns used for model highlighting and shadows.
 */
public enum VisualPattern {
    NONE(Color.Standard.TRANSPARENT, Color.Standard.TRANSPARENT),
    FRIENDLY(Color.Standard.GREEN, Color.Standard.DARK_GREEN),
    NEUTRAL(Color.Standard.BLUE, Color.Standard.DARK_BLUE),
    ENEMY(Color.Standard.RED, Color.Standard.DARK_RED),
    FRIENDLY_BUILDING(Color.Standard.GREEN, Color.Standard.DARK_GREEN),
    NEUTRAL_BUILDING(Color.Standard.BLUE, Color.Standard.DARK_BLUE),
    ENEMY_BUILDING(Color.Standard.RED, Color.Standard.DARK_RED);

    public final Color.Linear selectedColor;
    public final Color.Linear hoveredColor;

    VisualPattern(Color.Standard selectedColor, Color.Standard hoveredColor) {
        this.selectedColor = selectedColor.linear();
        this.hoveredColor = hoveredColor.linear();
    }
}
