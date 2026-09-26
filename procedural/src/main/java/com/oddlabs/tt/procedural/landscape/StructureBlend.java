package com.oddlabs.tt.procedural.landscape;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;

/**
 * Terrain blend layer containing structure diffuse and normal layers.
 */
public final class StructureBlend extends BlendInfo {
    private final Layer structureLayer;
    private final Layer normalLayer;
    private final float alphaPower;

    public StructureBlend(Layer structureLayer, Layer normalLayer,
            Channel alphaChannel) {
        this(structureLayer, normalLayer, alphaChannel, 1.0f);
    }

    public StructureBlend(Layer structureLayer, Layer normalLayer,
            Channel alphaChannel, float alphaPower) {
        super(alphaChannel);
        this.structureLayer = structureLayer;
        this.normalLayer = normalLayer;
        this.alphaPower = alphaPower;
    }

    public Layer getStructureLayer() {
        return structureLayer;
    }

    public Layer getNormalLayer() {
        return normalLayer;
    }

    public float getAlphaPower() {
        return alphaPower;
    }
}
