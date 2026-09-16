package com.oddlabs.tt.procedural.landscape;

import com.oddlabs.procedural.Channel;

/**
 * Base definition of terrain layer blending information.
 */
public abstract class BlendInfo {
    private final Channel alphaChannel;

    protected BlendInfo(Channel alphaChannel) {
        this.alphaChannel = alphaChannel;
    }

    public Channel getAlphaChannel() {
        return alphaChannel;
    }
}
