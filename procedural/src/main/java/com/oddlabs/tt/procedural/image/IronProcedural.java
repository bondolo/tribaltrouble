package com.oddlabs.tt.procedural.image;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.procedural.landscape.LandscapeConfig;
import com.oddlabs.tt.procedural.noise.Midpoint;
import com.oddlabs.tt.procedural.noise.Voronoi;

/**
 * Procedural generator for iron ore diffuse and normal map layers.
 */
public final class IronProcedural {
    public static final int TEXTURE_SIZE = 128;

    /**
     * Record holding iron diffuse and normal map layers.
     */
    public record IronLayers(Layer diffuse, Layer normalMap) {
    }

    private IronProcedural() {
    }

    /**
     * Generates the iron diffuse and normal map layers.
     */
    public static IronLayers generate() {
        int seed = LandscapeConfig.LANDSCAPE_SEED;

        Channel rock_bump = new Voronoi(TEXTURE_SIZE, 8, 8, 1, 1f, seed).getDistance(-1f, 0f, 0f).multiply(.49f);
        rock_bump.channelAdd(new Voronoi(TEXTURE_SIZE, 8, 8, 1, 1f, seed + 1).getDistance(-1f, 0f, 0f).multiply(.49f));
        Channel noise = new Midpoint(TEXTURE_SIZE, 7, .4f, seed).toChannel();
        Channel noise2 = new Midpoint(TEXTURE_SIZE, 3, .45f, seed).toChannel();
        rock_bump.channelAdd(noise.copy().multiply(.02f));
        Layer rock = noise.copy().dynamicRange(.5f, .75f).toLayer();
        Channel rustAlpha = noise2.copy().gamma2().invert();
        Layer rust = new Layer(new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(.4f), new Channel(TEXTURE_SIZE,
                TEXTURE_SIZE).fill(.15f), new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(0f), rustAlpha);
        rock.layerBlend(rust);
        rock.bump(rock_bump, TEXTURE_SIZE / 256f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f).gamma(.5f).dynamicRange(0f, .75f);
        Channel stainAlpha = noise2.copy().gamma(.75f).rotate(90);
        Layer stain = new Layer(noise2.copy().multiply(.4f), noise2.copy().multiply(.15f), noise2.copy().multiply(0f),
                stainAlpha);
        stain.bump(noise2.copy(), 8f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f);
        rock.layerBlend(stain);

        Channel specular = new Channel(TEXTURE_SIZE, TEXTURE_SIZE).fill(0.65f).channelSubtract(rustAlpha)
                .channelSubtract(stainAlpha).clip();

        Layer normalMapLayer = rock_bump.toNormalMap(2.5f, specular);
        return new IronLayers(rock, normalMapLayer);
    }
}
