package com.oddlabs.tt.procedural;

import org.jspecify.annotations.NonNull;

/**
 * Terrain blend layer containing structure diffuse and normal images.
 */
public final class StructureBlend extends BlendInfo {
    private final @NonNull GLIntImage structureImage;
    private final @NonNull GLIntImage normalImage;

    public StructureBlend(@NonNull GLIntImage structureImage, @NonNull GLIntImage normalImage,
            @NonNull GLByteImage alphaImage) {
        super(alphaImage);
        this.structureImage = structureImage;
        this.normalImage = normalImage;
    }

    public @NonNull GLIntImage getStructureImage() {
        return structureImage;
    }

    public @NonNull GLIntImage getNormalImage() {
        return normalImage;
    }
}
