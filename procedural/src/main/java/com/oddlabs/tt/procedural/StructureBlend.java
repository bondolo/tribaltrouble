package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import org.jspecify.annotations.NonNull;

/**
 * Terrain blend layer containing structure diffuse and normal layers.
 */
public final class StructureBlend extends BlendInfo {
    private final @NonNull Layer structureLayer;
    private final @NonNull Layer normalLayer;

    public StructureBlend(@NonNull Layer structureLayer, @NonNull Layer normalLayer,
            @NonNull Channel alphaChannel) {
        super(alphaChannel);
        this.structureLayer = structureLayer;
        this.normalLayer = normalLayer;
    }

    public @NonNull Layer getStructureLayer() {
        return structureLayer;
    }

    public @NonNull Layer getNormalLayer() {
        return normalLayer;
    }
}
