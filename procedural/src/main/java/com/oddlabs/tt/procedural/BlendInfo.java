package com.oddlabs.tt.procedural;

import org.jspecify.annotations.NonNull;

/**
 * Base definition of terrain layer blending information.
 */
public abstract class BlendInfo {
    private final @NonNull GLByteImage sourceImage;

    protected BlendInfo(@NonNull GLByteImage sourceImage) {
        this.sourceImage = sourceImage;
    }

    public @NonNull GLByteImage getSourceImage() {
        return sourceImage;
    }
}
