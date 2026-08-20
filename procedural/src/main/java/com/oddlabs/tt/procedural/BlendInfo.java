package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import org.jspecify.annotations.NonNull;

/**
 * Base definition of terrain layer blending information.
 */
public abstract class BlendInfo {
    private final @NonNull Channel alphaChannel;

    protected BlendInfo(@NonNull Channel alphaChannel) {
        this.alphaChannel = alphaChannel;
    }

    public @NonNull Channel getAlphaChannel() {
        return alphaChannel;
    }
}
