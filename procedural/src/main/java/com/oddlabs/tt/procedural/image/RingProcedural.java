package com.oddlabs.tt.procedural.image;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.procedural.Tools;

/**
 * Procedural generator for selection ring decal layers.
 */
public final class RingProcedural {
    private RingProcedural() {
    }

    private static Channel generateLUT(int size, float[][] gradient_list) {
        Channel channel = new Channel(size, 1);
        int index_max = gradient_list.length - 1;
        for (int i = 0; i < size; i++) {
            float pos = (i + 0.5f) / size;
            float radius = pos * 0.5f;

            int index = 0;
            while (radius >= gradient_list[index][0] && index < index_max) {
                index++;
            }

            float value;
            if (radius < gradient_list[0][0]) {
                value = gradient_list[0][1];
            } else if (radius >= gradient_list[index_max][0]) {
                value = gradient_list[index_max][1];
            } else {
                float fraction = (radius - gradient_list[index - 1][0]) / (gradient_list[index][0] - gradient_list[index
                        - 1][0]);
                value = Tools.interpolateLinear(gradient_list[index - 1][1], gradient_list[index][1], fraction);
            }
            channel.putPixel(i, 0, value);
        }
        return channel;
    }

    /**
     * Generates a selection ring layer for DecalShader.
     */
    public static Layer generate(int size, float[][] ring_parms) {
        Channel channel_ring = generateLUT(size, ring_parms);
        Channel channel_black = new Channel(size, 1).fill(0f);
        Channel channel_white = new Channel(size, 1).fill(1f);
        return new Layer(channel_ring.copy(), channel_black.copy(), channel_black.copy(), channel_white.copy());
    }
}
