package com.oddlabs.tt.procedural;

import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/**
 * Blend information representing directional lighting contribution on terrain.
 */
public final class BlendLighting extends BlendInfo {

    private final Color.@NonNull Linear color;

    public BlendLighting(@NonNull GLByteImage alpha_image, @NonNull Color color) {
        super(alpha_image);
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
    }

    public @NonNull Color getColor() {
        return color;
    }
}
