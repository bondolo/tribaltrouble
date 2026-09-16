package com.oddlabs.tt.procedural.image;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.procedural.noise.Voronoi;
import com.oddlabs.tt.procedural.shape.Ring;

/**
 * Procedural generator for stylized particle smoke and puff layers.
 */
public final class SmokeProcedural {
    public static final int TEXTURE_SIZE = 128;

    private SmokeProcedural() {
    }

    /**
     * Generates a stylized smoke puff layer.
     */
    public static Layer generate(int seed, float baseBrightness, float alphaMultiplier) {
        Channel voronoi = new Voronoi(TEXTURE_SIZE, 4, 4, 1, 1f, seed).getDistance(-1f, 1f, 0f);

        Channel smoke_alpha = new Ring(TEXTURE_SIZE, TEXTURE_SIZE, new float[][]{{0f, 1f}, {0.4f, 0f}},
                Ring.Interpolation.SMOOTH).toChannel().gamma(3.0f);
        smoke_alpha.channelMultiply(voronoi.copy().dynamicRange(0.85f, 1.0f));

        if (alphaMultiplier != 1.0f) {
            smoke_alpha.brightness(alphaMultiplier);
        }

        Channel smoke_color = new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(baseBrightness);
        Channel smoke_bump = voronoi.copy().gamma(0.5f).smooth(3).dynamicRange(0.0f, 1.0f).channelMultiply(smoke_alpha);
        smoke_color.bump(smoke_bump, 0.5f, -0.5f, 0.15f, 0.4f, 0.7f);

        return new Layer(smoke_color.copy(), smoke_color.copy(), smoke_color.copy(), smoke_alpha);
    }
}
