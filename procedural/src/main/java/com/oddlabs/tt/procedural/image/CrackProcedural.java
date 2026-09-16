package com.oddlabs.tt.procedural.image;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.procedural.landscape.LandscapeConfig;
import com.oddlabs.tt.procedural.noise.Voronoi;
import com.oddlabs.tt.procedural.shape.Ring;

/**
 * Procedural generator for ground-impact decal crack layers.
 */
public final class CrackProcedural {
    public static final int TEXTURE_SIZE = 128;

    private CrackProcedural() {
    }

    /**
     * Generates the ground-impact crack layer.
     */
    public static Layer generate() {
        int seed = LandscapeConfig.LANDSCAPE_SEED;
        Channel voronoi = new Voronoi(TEXTURE_SIZE, 5, 5, 1, 1f, seed).getDistance(-1f, 1f, 0f);
        Channel borders = voronoi.dynamicRange().threshold(0.0f, 0.05f);
        Channel falloff = new Ring(TEXTURE_SIZE, TEXTURE_SIZE, new float[][]{{0f, 1f}, {0.35f, 1f}, {0.5f, 0f}},
                Ring.Interpolation.SMOOTH).toChannel();
        Channel alpha = borders.channelMultiply(falloff);
        Channel red = falloff.copy();
        Channel green = falloff.copy().multiply(0.25f);
        Channel blue = new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(0f);
        return new Layer(red, green, blue, alpha);
    }
}
