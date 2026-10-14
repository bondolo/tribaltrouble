package com.oddlabs.tt.client.landscape;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;

/**
 * Terrain blend layer containing structure diffuse and normal layers.
 *
 * @param structureLayer the diffuse structure texture layer
 * @param normalLayer the normal structure texture layer
 * @param alphaChannel the alpha coverage channel
 * @param alphaPower the power exponent applied to the alpha channel
 */
record StructureBlend(Layer structureLayer, Layer normalLayer, Channel alphaChannel, float alphaPower)
        implements BlendInfo {
}
