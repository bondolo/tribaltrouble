package com.oddlabs.tt.client.landscape;

import com.oddlabs.procedural.Channel;
import com.oddlabs.util.Color;

/**
 * Blend information representing ambient occlusion applied over the landscape.
 *
 * @param alphaChannel the alpha coverage channel
 * @param color the linear occlusion color
 */
record BlendOcclusion(Channel alphaChannel, Color.Linear color) implements BlendInfo {

    BlendOcclusion(Channel alphaChannel, Color color) {
        this(alphaChannel, color instanceof Color.Linear linear ? linear : new Color.Linear(color));
    }
}
