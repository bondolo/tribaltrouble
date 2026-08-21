package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.util.Color;

/**
 * Blend information representing directional lighting contribution on terrain.
 */
public final class BlendLighting extends BlendInfo {

    private final Color.Linear color;

    public BlendLighting(Channel alphaChannel, Color color) {
        super(alphaChannel);
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
    }

    public Color getColor() {
        return color;
    }
}
