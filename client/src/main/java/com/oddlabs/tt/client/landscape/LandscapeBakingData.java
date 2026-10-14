package com.oddlabs.tt.client.landscape;

import com.oddlabs.procedural.Layer;

/**
 * Transient baking data required by the visual pipeline to bake colormaps and detail textures.
 *
 * @param blendInfos the blend layer definitions for terrain texture splatting
 * @param detail the high-frequency detail texture layer
 * @param detailNormal the high-frequency detail normal map layer
 */
record LandscapeBakingData(
                           BlendInfo[] blendInfos,
                           Layer detail,
                           Layer detailNormal) {
}
