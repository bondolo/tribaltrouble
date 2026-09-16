package com.oddlabs.tt.procedural.image;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.procedural.noise.Perlin;

/**
 * Procedural generator for animated turbulence noise layers.
 */
public final class NoiseProcedural {
    private NoiseProcedural() {
    }

    /**
     * Generates a Perlin noise layer.
     */
    public static Layer generate(int size, long seed) {
        Perlin perlin = new Perlin(size, size, 4, 4, 0.5f, 4, seed, Perlin.Interpolation.SMOOTH,
                Perlin.Summation.NORMAL);
        return perlin.toLayer();
    }
}
