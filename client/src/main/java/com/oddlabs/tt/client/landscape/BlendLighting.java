package com.oddlabs.tt.client.landscape;

import com.oddlabs.procedural.Channel;
import com.oddlabs.util.Color;

/**
 * Blend information representing directional lighting contribution on terrain.
 *
 * @param alphaChannel the alpha coverage channel
 * @param color the linear lighting color
 */
record BlendLighting(Channel alphaChannel, Color.Linear color) implements BlendInfo {

    BlendLighting(Channel alphaChannel, Color color) {
        this(alphaChannel, color instanceof Color.Linear linear ? linear : new Color.Linear(color));
    }
}
