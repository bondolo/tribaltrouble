package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;

/**
 * Terrain blend layer containing structure diffuse and normal layers.
 */
public final class StructureBlend extends BlendInfo {
    private final Layer structureLayer;
    private final Layer normalLayer;

    public StructureBlend(Layer structureLayer, Layer normalLayer,
            Channel alphaChannel) {
        super(alphaChannel);
        this.structureLayer = structureLayer;
        this.normalLayer = normalLayer;
    }

    public Layer getStructureLayer() {
        return structureLayer;
    }

    public Layer getNormalLayer() {
        return normalLayer;
    }
}
