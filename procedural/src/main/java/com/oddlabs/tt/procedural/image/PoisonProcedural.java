package com.oddlabs.tt.procedural.image;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.procedural.landscape.LandscapeConfig;
import com.oddlabs.tt.procedural.noise.Midpoint;
import com.oddlabs.tt.procedural.shape.Hill;

/**
 * Procedural generator for poison bubble effect layers.
 */
public final class PoisonProcedural {
    public static final int TEXTURE_SIZE = 128;

    private PoisonProcedural() {
    }

    /**
     * Generates a poison bubble effect layer.
     */
    public static Layer generate() {
        int seed = LandscapeConfig.LANDSCAPE_SEED;

        Channel blob = new Hill(TEXTURE_SIZE, Hill.Shape.CIRCLE).toChannel();
        Channel noise = new Midpoint(TEXTURE_SIZE, 2, .45f, seed).toChannel();
        Channel poison_alpha = noise.copy().channelMultiply(blob.copy().dynamicRange(.25f, 1f)).channelSubtract(blob
                .copy().invert().brightness(.25f));

        return new Layer(noise.copy().rotate(90).dynamicRange(.5f, 1f), new Channel(TEXTURE_SIZE, TEXTURE_SIZE)
                .fill(1f), new Channel(TEXTURE_SIZE, TEXTURE_SIZE), poison_alpha.brightness(.75f));
    }
}
