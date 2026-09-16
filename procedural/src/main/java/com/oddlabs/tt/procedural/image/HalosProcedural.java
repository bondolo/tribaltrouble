package com.oddlabs.tt.procedural.image;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.procedural.Tools;

import java.util.EnumMap;
import java.util.Map;

/**
 * Procedural generator for unit selection and shadow halo layers.
 */
public final class HalosProcedural {
    /**
     * Types of halo layers that can be generated.
     */
    public enum HaloType {
        SHADOWED,
        SELECTED
    }

    private HalosProcedural() {
    }

    private static Channel generateLUT(int size, float[][] gradient_list, boolean smooth) {
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
                value = smooth
                        ? Tools.interpolateSmooth(gradient_list[index - 1][1], gradient_list[index][1], fraction)
                        : Tools.interpolateLinear(gradient_list[index - 1][1], gradient_list[index][1], fraction);
            }
            channel.putPixel(i, 0, value);
        }
        return channel;
    }

    /**
     * Generates halo layers for shadowed and selected states.
     */
    public static Map<HaloType, Layer> generate(int size, float[][] shadow_parms, float[][] ring_parms) {
        Channel channel_shadow = generateLUT(size, shadow_parms, true);
        Channel channel_ring = generateLUT(size, ring_parms, false);
        Channel channel_black = new Channel(size, 1).fill(0f);
        Channel channel_white = new Channel(size, 1).fill(1f);

        Map<HaloType, Layer> layers = new EnumMap<>(HaloType.class);
        layers.put(HaloType.SHADOWED, new Layer(channel_black.copy(), channel_shadow.copy(), channel_black.copy(),
                channel_white.copy()));
        layers.put(HaloType.SELECTED, new Layer(channel_ring.copy(), channel_shadow.copy(), channel_black.copy(),
                channel_white.copy()));
        return layers;
    }
}
