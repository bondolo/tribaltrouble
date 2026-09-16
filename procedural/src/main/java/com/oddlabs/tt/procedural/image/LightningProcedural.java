package com.oddlabs.tt.procedural.image;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.procedural.shape.Gradient;

/**
 * Procedural generator for lightning bolt effect layers.
 */
public final class LightningProcedural {
    public static final int TEXTURE_SIZE = 128;

    private LightningProcedural() {
    }

    /**
     * Generates a lightning strike bolt layer.
     */
    public static Layer generate() {
        Channel gradient = new Gradient(TEXTURE_SIZE, TEXTURE_SIZE, new float[][]{{0f, 0f}, {.47f, .25f}, {.5f, 1f}, {
                .53f, .25f}, {1f, 0f}}, Gradient.Orientation.HORIZONTAL, Gradient.Interpolation.SMOOTH).toChannel();
        return new Layer(new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(1f), new Channel(TEXTURE_SIZE,
                TEXTURE_SIZE).fill(1f), gradient.copy(), gradient.copy());
    }
}
