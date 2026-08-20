package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/**
 * Blend information representing ambient occlusion applied over the landscape.
 */
public final class BlendOcclusion extends BlendInfo {

    private final Color.@NonNull Linear color;

    public BlendOcclusion(@NonNull Channel alphaChannel, @NonNull Color color) {
        super(alphaChannel);
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
    }

    public @NonNull Color getColor() {
        return color;
    }
}
