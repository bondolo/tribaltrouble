package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.util.Color;

/**
 * Blend information representing ambient occlusion applied over the landscape.
 */
public final class BlendOcclusion extends BlendInfo {

    private final Color.Linear color;

    public BlendOcclusion(Channel alphaChannel, Color color) {
        super(alphaChannel);
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
    }

    public Color getColor() {
        return color;
    }
}
