package com.oddlabs.tt.client.landscape;

import com.oddlabs.procedural.Channel;

/**
 * Base definition of terrain layer blending information.
 */
sealed interface BlendInfo permits StructureBlend, BlendLighting, BlendOcclusion {
    /**
     * Returns the alpha coverage mask for this blend pass.
     *
     * @return the alpha mask channel
     */
    Channel alphaChannel();
}
