package com.oddlabs.tt.client.landscape;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.engine.render.state.SceneLighting;
import com.oddlabs.tt.procedural.noise.Midpoint;
import com.oddlabs.tt.procedural.noise.Voronoi;
import com.oddlabs.tt.simulation.SimulationConfig;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.util.Color;

import java.util.EnumMap;
import java.util.Map;

/**
 * Generates procedural texture splatting layers, normal maps, and blend definitions for landscape rendering.
 */
final class LandscapeVisualGenerator {
    private static final boolean DEBUG = false;
    private static final int STRUCTURE_SIZE = 256;
    static final int DETAIL_SIZE = 256;
    private static final int DEFAULT_NOISE_SEED = 1;
    private static final int STRUCTURE_SEED = 42; // must be constant; otherwise distinct repeating patterns might appear
    private static final float WORLEY_VORONI_C1 = -1f;
    private static final float WORLEY_VORONI_C2 = 1f;
    private static final float WORLEY_VORONI_C3 = 0f;

    // Native terrain colors (RGB)
    private static final Color NATIVE_SAND_COLOR = new Color.Standard(0xFF_FF_E6_CC);
    private static final Color NATIVE_DIRT_COLOR = new Color.Standard(0xFF_FF_B3_80);
    private static final Color NATIVE_GRASS_COLOR = new Color.Standard(0xFF_33_73_00);
    private static final Color NATIVE_ROCK_TINT = new Color.Standard(0xFF_FF_CC_99);

    // Viking terrain colors (RGB)
    private static final Color VIKING_GRAVEL_COLOR = new Color.Standard(0xFF_B3_8C_66);
    private static final Color VIKING_SOIL_COLOR = new Color.Standard(0xFF_A6_80_59);
    private static final Color VIKING_GRASS_COLOR = new Color.Standard(0xFF_0E_73_00); // Saturated green matching legacy baseline
    private static final Color VIKING_SNOW_COLOR = new Color.Standard(0xFF_F2_F2_F2);

    // Misc colors
    private static final float DETAIL_GREY = 0.5f;

    private static final Map<Terrain, Color.Standard> SEA_BOTTOM_COLOR = new EnumMap<>(Map.of(
            Terrain.NATIVE, new Color.Standard(0xFF_73_40_99),
            Terrain.VIKING, Color.Standard.BLACK
    ));
    private static final Color BLEND_LIGHTING_COLOR = new Color.Standard(0xFF_FF_E6_99); // 1.0, 0.9, 0.6
    private static final float LINEAR_ALPHA_POWER = 1.0f;

    private record StructureLayers(Layer diffuse, Layer normal) {
    }

    private record GeneratedStructures(StructureLayers[] layers, Layer detail, Layer detailNormal) {
    }

    private LandscapeVisualGenerator() {
    }

    /**
     * Generates the baking data required to bake landscape colormaps and detail textures using canonical scene
     * lighting.
     *
     * @param landscape the landscape containing topography and island configuration
     * @param detailAlphaValue the alpha multiplier for the high-frequency detail layer
     * @param grassAlphaPower the alpha exponent for the grass blending layer
     * @return the generated visual baking data
     */
    static LandscapeBakingData generate(
            Landscape landscape,
            float detailAlphaValue,
            float grassAlphaPower) {
        return generate(landscape, SceneLighting.CANONICAL, detailAlphaValue, grassAlphaPower);
    }

    /**
     * Generates the baking data required to bake landscape colormaps and detail textures.
     *
     * @param landscape the landscape containing topography and island configuration
     * @param lighting scene lighting defining light direction for directional hill shading
     * @param detailAlphaValue the alpha multiplier for the high-frequency detail layer
     * @param grassAlphaPower the alpha exponent for the grass blending layer
     * @return the generated visual baking data
     */
    static LandscapeBakingData generate(
            Landscape landscape,
            SceneLighting lighting,
            float detailAlphaValue,
            float grassAlphaPower) {
        IslandConfig config = landscape.config();
        Terrain terrain = config.terrain();
        int metersPerWorld = config.metersPerWorld();
        int unitGridsPerWorld = metersPerWorld / HeightMap.METERS_PER_UNIT_GRID;
        int metersPerHeightUnit = metersPerWorld / unitGridsPerWorld;
        float vegetationAmount = 0.25f + 0.75f * config.vegetation();
        int heightScale = landscape.heightScale();
        float[] heightPixels = landscape.heightmap();

        // Reconstruct unscaled normalized height map (0..1)
        Channel heightMap = new Channel(unitGridsPerWorld, unitGridsPerWorld);
        float[] unscaled = heightMap.getPixels();
        for (int i = 0; i < heightPixels.length; i++) {
            unscaled[i] = heightPixels[i] / heightScale;
        }

        Channel slope = heightMap.copy().lineart();
        Channel relheight = heightMap.copy().relativeIntensityNormalized(Math.max(1, unitGridsPerWorld >> 5));

        // Generate shared voronoi and noise maps
        Channel voronoi4 = new Voronoi(STRUCTURE_SIZE, 4, 4, 1, 1f, STRUCTURE_SEED, false)
                .getDistance(WORLEY_VORONI_C1, WORLEY_VORONI_C2, WORLEY_VORONI_C3);
        Channel voronoi8 = new Voronoi(STRUCTURE_SIZE, 8, 8, 1, 1f, STRUCTURE_SEED, false)
                .getDistance(WORLEY_VORONI_C1, WORLEY_VORONI_C2, WORLEY_VORONI_C3);
        Channel voronoi16 = new Voronoi(STRUCTURE_SIZE, 16, 16, 1, 1f, STRUCTURE_SEED, false)
                .getDistance(WORLEY_VORONI_C1, WORLEY_VORONI_C2, WORLEY_VORONI_C3);
        Channel voronoi32 = new Voronoi(STRUCTURE_SIZE, 32, 32, 1, 1f, STRUCTURE_SEED, false)
                .getDistance(WORLEY_VORONI_C1, WORLEY_VORONI_C2, WORLEY_VORONI_C3);

        Channel noise8 = new Midpoint(STRUCTURE_SIZE, 3, 0.45f, STRUCTURE_SEED).toChannel();
        Channel noise256 = new Midpoint(STRUCTURE_SIZE, 8, 1f, STRUCTURE_SEED).toChannel();

        GeneratedStructures structures = switch (terrain) {
            case NATIVE -> {
                var natives = generateStructuresNative(terrain, detailAlphaValue, voronoi4, voronoi8, voronoi16,
                        voronoi32, noise8, noise256);
                ProgressListener.progress();
                yield natives;
            }
            case VIKING -> {
                var vikings = generateStructuresViking(terrain, detailAlphaValue, voronoi4, voronoi8, voronoi16,
                        voronoi32, noise8, noise256);
                ProgressListener.progress();
                yield vikings;
            }
        };
        StructureLayers[] layers = structures.layers();

        Channel[] alphaMaps = new Channel[7];
        generateAlphas(terrain, unitGridsPerWorld, metersPerHeightUnit, heightScale, vegetationAmount,
                landscape.accessThreshold(), landscape.buildThreshold(), heightMap, slope, relheight, lighting,
                alphaMaps);
        ProgressListener.progress();

        BlendInfo[] blendInfos = switch (terrain) {
            case NATIVE -> new BlendInfo[]{
                    new StructureBlend(layers[0].diffuse(), layers[0].normal(), new Channel(1, 1).fill(1f),
                            LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[1].diffuse(), layers[1].normal(), alphaMaps[0], LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[2].diffuse(), layers[2].normal(), alphaMaps[1], LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[3].diffuse(), layers[3].normal(), alphaMaps[2], LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[4].diffuse(), layers[4].normal(), alphaMaps[3], grassAlphaPower),
                    new BlendLighting(alphaMaps[4], BLEND_LIGHTING_COLOR),
                    new StructureBlend(layers[5].diffuse(), layers[5].normal(), alphaMaps[5], LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[6].diffuse(), layers[6].normal(), alphaMaps[6], LINEAR_ALPHA_POWER)
            };
            case VIKING -> new BlendInfo[]{
                    new StructureBlend(layers[0].diffuse(), layers[0].normal(), new Channel(1, 1).fill(1f),
                            LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[1].diffuse(), layers[1].normal(), alphaMaps[0], LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[2].diffuse(), layers[2].normal(), alphaMaps[1], LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[3].diffuse(), layers[3].normal(), alphaMaps[2], grassAlphaPower),
                    new StructureBlend(layers[4].diffuse(), layers[4].normal(), alphaMaps[3], LINEAR_ALPHA_POWER),
                    new BlendLighting(alphaMaps[4], BLEND_LIGHTING_COLOR),
                    new StructureBlend(layers[5].diffuse(), layers[5].normal(), alphaMaps[5], LINEAR_ALPHA_POWER),
                    new StructureBlend(layers[6].diffuse(), layers[6].normal(), alphaMaps[6], LINEAR_ALPHA_POWER)
            };
        };

        return new LandscapeBakingData(blendInfos, structures.detail(), structures.detailNormal());
    }

    private static GeneratedStructures generateStructuresNative(Terrain terrain, float detailAlphaValue,
            Channel voronoi4, Channel voronoi8, Channel voronoi16, Channel voronoi32, Channel noise8,
            Channel noise256) {
        var structures = new StructureLayers[7];
        ProgressListener.progress(1 / 8f);

        structures[0] = genSand(STRUCTURE_SIZE, noise8.copy(), noise256.copy());
        structures[1] = genDirt(STRUCTURE_SIZE, noise8.copy(), noise256.copy(), voronoi32.copy());
        structures[2] = genRubble(STRUCTURE_SIZE, noise8.copy(), voronoi4.copy(), voronoi8.copy(),
                voronoi16.copy(), structures[1].diffuse().copy());
        structures[4] = genGrass(STRUCTURE_SIZE, NATIVE_GRASS_COLOR, noise8.copy(), noise256.copy());
        structures[3] = genRock(STRUCTURE_SIZE, noise8.copy(), noise256.copy(), voronoi4.copy(),
                voronoi8.copy(), voronoi16.copy(), structures[2].diffuse.copy(), structures[4].diffuse.copy());
        structures[5] = genBlack();
        structures[6] = genSeabottom(terrain, STRUCTURE_SIZE);

        StructureLayers structureDetail = genDetail(DETAIL_SIZE, detailAlphaValue, STRUCTURE_SEED,
                noise8.copy());
        return new GeneratedStructures(structures, structureDetail.diffuse(), structureDetail.normal());
    }

    private static GeneratedStructures generateStructuresViking(Terrain terrain, float detailAlphaValue,
            Channel voronoi4, Channel voronoi8, Channel voronoi16, Channel voronoi32,
            Channel noise8, Channel noise256) {
        var structures = new StructureLayers[7];
        ProgressListener.progress(1 / 8f);

        structures[0] = genGravel(STRUCTURE_SIZE, noise8.copy(), noise256.copy());
        structures[1] = genSoil(STRUCTURE_SIZE, noise8.copy(), noise256.copy(), voronoi32.copy());
        structures[3] = genGrass(STRUCTURE_SIZE, VIKING_GRASS_COLOR, noise8.copy(), noise256.copy());
        structures[3].diffuse.multiply(.75f, .9f, 1.1f);
        structures[2] = genCliff(STRUCTURE_SIZE, noise8.copy(), noise256.copy(), voronoi4.copy(),
                voronoi8.copy(), voronoi16.copy(), structures[1].diffuse.copy(), structures[3].diffuse.copy());
        structures[4] = genSnow(STRUCTURE_SIZE, noise8.copy(), noise256.copy());
        structures[5] = genBlack();
        structures[6] = genSeabottom(terrain, STRUCTURE_SIZE);

        StructureLayers structureDetail = genDetail(DETAIL_SIZE, detailAlphaValue, STRUCTURE_SEED,
                noise8.copy());
        return new GeneratedStructures(structures, structureDetail.diffuse(), structureDetail.normal());
    }

    private static Layer toNormalMapWithZeroSpecular(Channel bump, float strength, int size) {
        return toNormalMapWithSpecular(bump, strength, 0f, size);
    }

    private static Layer toNormalMapWithSpecular(Channel bump, float strength, float specular, int size) {
        Channel spec = new Channel(size, size).fill(specular);
        return bump.toNormalMap(strength, spec);
    }

    private static Layer toLayerWithAlpha(Layer layer, float alpha) {
        Channel a = new Channel(layer.r.getWidth(), layer.r.getHeight()).fill(alpha);
        return new Layer(layer.r, layer.g, layer.b, a);
    }

    private static StructureLayers genSand(int size, Channel noise8, Channel noise256) {
        Channel empty = new Channel(size, size).fill(1f);
        Channel sand_bump1 = noise8.brightness(0.75f);
        Channel sand_bump2 = noise256.brightness(0.25f);
        Layer sand = new Layer(empty.copy().fill(NATIVE_SAND_COLOR.r()), empty.copy().fill(NATIVE_SAND_COLOR.g()), empty
                .copy().fill(NATIVE_SAND_COLOR.b()));
        Channel bump = sand_bump1.channelAdd(sand_bump2);
        sand.bump(bump, size / 1024f, 0f, 0.1f, 1f, 1f, 1f, 0f, 0f, 0f);
        if (DEBUG) sand.saveAsPNG("structure_sand");

        return new StructureLayers(toLayerWithAlpha(sand, 1.0f), toNormalMapWithSpecular(bump, 0.3f, 0.15f, size));
    }

    private static StructureLayers genGravel(int size, Channel noise8, Channel noise256) {
        Channel empty = new Channel(size, size).fill(1f);
        Channel gravel_bump1 = noise8.brightness(0.5f);
        Channel gravel_bump2 = noise256.brightness(0.5f);
        Layer gravel = new Layer(empty.copy().fill(VIKING_GRAVEL_COLOR.r()), empty.copy().fill(VIKING_GRAVEL_COLOR.g()),
                empty.copy().fill(VIKING_GRAVEL_COLOR.b()));
        Channel bump = gravel_bump1.channelAdd(gravel_bump2);
        gravel.bump(bump, size / 1024f, 0f, 0.1f, 1f, 1f, 1f, 0f, 0f, 0f);
        if (DEBUG) gravel.saveAsPNG("structure_gravel");

        return new StructureLayers(toLayerWithAlpha(gravel, 1.0f), toNormalMapWithSpecular(bump, 0.6f, 0.1f, size));
    }

    private static StructureLayers genDirt(int size, Channel noise8, Channel noise256,
            Channel voronoi32) {
        Channel empty = new Channel(size, size).fill(1f);
        Channel dirt_bump1 = noise8.brightness(0.8f);
        Channel dirt_bump2 = noise256.brightness(0.1f);
        Channel dirt_bump3 = voronoi32.brightness(0.1f);
        Layer dirt = new Layer(empty.copy().fill(NATIVE_DIRT_COLOR.r()), empty.copy().fill(NATIVE_DIRT_COLOR.g()), empty
                .copy().fill(NATIVE_DIRT_COLOR.b()));
        Channel dirt_bump = dirt_bump1.channelAdd(dirt_bump2).channelAdd(dirt_bump3);
        dirt_bump.perturb(noise8, 0.05f);
        dirt.bump(dirt_bump, size / 128f, 0f, 0.5f, 1f, 1f, 1f, 0f, 0f, 0f);
        if (DEBUG) dirt.saveAsPNG("structure_dirt");

        return new StructureLayers(toLayerWithAlpha(dirt, 1.0f), toNormalMapWithSpecular(dirt_bump, 0.6f, 0.1f, size));
    }

    private static StructureLayers genSoil(int size, Channel noise8, Channel noise256,
            Channel voronoi32) {
        Channel empty = new Channel(size, size).fill(1f);
        Channel soil_bump1 = noise8.brightness(0.8f);
        Channel soil_bump2 = noise256.brightness(0.1f);
        Channel soil_bump3 = voronoi32.brightness(0.1f);
        Layer soil = new Layer(empty.copy().fill(VIKING_SOIL_COLOR.r()), empty.copy().fill(VIKING_SOIL_COLOR.g()), empty
                .copy().fill(VIKING_SOIL_COLOR.b()));
        Channel soil_bump = soil_bump1.channelAdd(soil_bump2).channelAdd(soil_bump3);
        soil_bump.perturb(noise8, 0.05f);
        soil.bump(soil_bump, size / 128f, 0f, 0.5f, 1f, 1f, 1f, 0f, 0f, 0f);
        if (DEBUG) soil.saveAsPNG("structure_soil");

        return new StructureLayers(toLayerWithAlpha(soil, 1.0f), toNormalMapWithSpecular(soil_bump, 1.5f, 0.1f, size));
    }

    private static StructureLayers genGrass(int size, Color grassColor, Channel noise8,
            Channel noise256) {
        Channel empty = new Channel(size, size).fill(1f);
        Channel grass_bump = noise8.copy().rotate(90).channelAdd(noise256.brightness(0.05f));
        Layer grass = new Layer(empty.copy().fill(grassColor.r()), empty.copy().fill(grassColor.g()), empty.copy().fill(
                grassColor.b()));
        grass.r.channelAdd(noise8.brightness(0.20f));
        grass.bump(grass_bump, size / 256f, 0f, 0.6f, 1f, 1f, 1f, 0f, 0f, 0f);
        if (DEBUG) grass.saveAsPNG("structure_grass");

        return new StructureLayers(toLayerWithAlpha(grass, 1.0f), getFlatNormal(size));
    }

    private static StructureLayers genRubble(int size, Channel noise8, Channel voronoi4,
            Channel voronoi8, Channel voronoi16, Layer rubble) {
        Channel rubble_bump1 = voronoi4.multiply(0.4f);
        Channel rubble_bump2 = voronoi8.multiply(0.3f);
        Channel rubble_bump3 = voronoi16.multiply(0.2f);
        Channel rubble_bump = rubble_bump1.channelAdd(rubble_bump2).channelAdd(rubble_bump3).dynamicRange();
        rubble_bump.perturb(noise8, 0.1f);
        rubble.multiply(.9f).bump(rubble_bump, size / 128f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f);
        if (DEBUG) rubble.saveAsPNG("structure_rubble");

        return new StructureLayers(toLayerWithAlpha(rubble, 1.0f), toNormalMapWithSpecular(rubble_bump, 1.5f, 0.35f,
                size));
    }

    private static StructureLayers genRock(int size, Channel noise8, Channel noise256,
            Channel voronoi4, Channel voronoi8, Channel voronoi16, Layer rubble,
            Layer grass) {
        Channel rock_bump1 = voronoi4.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.35f);
        Channel rock_bump2 = voronoi8.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.3f);
        Channel rock_bump3 = voronoi16.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.2f);
        Channel rock_bump = rock_bump1.channelAdd(rock_bump2).channelAdd(rock_bump3).channelAdd(noise256.multiply(
                0.15f));
        rock_bump.dynamicRange().perturb(noise8, 0.1f);
        Layer rock = rubble.copy();
        rock.toHSV();
        rock.r = noise8.copy().dynamicRange(0.05f, 0.1f);
        rock.toRGB();
        rock.layerBlend(rubble.multiply(NATIVE_ROCK_TINT.r(), NATIVE_ROCK_TINT.g(), NATIVE_ROCK_TINT.b()), noise8
                .gamma8().invert().contrast(4f));
        rock.layerBlend(grass.multiply(0.5f), noise8.rotate(90).multiply(0.5f)); // grass tint
        rock.bump(rock_bump, size / 192f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f);
        rock.gamma2().multiply(0.9f);
        if (DEBUG) rock.saveAsPNG("structure_rock");

        return new StructureLayers(toLayerWithAlpha(rock, 1.0f), toNormalMapWithSpecular(rock_bump, 2.5f, 0.35f, size));
    }

    private static StructureLayers genCliff(int size, Channel noise8, Channel noise256,
            Channel voronoi4, Channel voronoi8, Channel voronoi16, Layer rubble,
            Layer grass) {
        Channel cliff_bump1 = voronoi4.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.3f);
        Channel cliff_bump2 = voronoi8.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.3f);
        Channel cliff_bump3 = voronoi16.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.25f);
        Channel cliff_bump = cliff_bump1.channelAdd(cliff_bump2).channelAdd(cliff_bump3).channelAdd(noise256.multiply(
                0.15f));
        cliff_bump.dynamicRange().perturb(noise8, 0.1f);
        Layer cliff = rubble.copy();
        cliff.toHSV();
        cliff.r = noise8.copy().dynamicRange(0.05f, 0.1f);
        cliff.g.multiply(0.75f);
        cliff.toRGB();
        cliff.layerBlend(rubble, noise8.gamma8().invert().contrast(4f));
        cliff.layerBlend(grass, noise8.rotate(90).multiply(0.75f));
        cliff.bump(cliff_bump, size / 192f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f);
        cliff.gamma2().multiply(0.9f);
        if (DEBUG) cliff.saveAsPNG("structure_cliff");

        return new StructureLayers(toLayerWithAlpha(cliff, 1.0f), toNormalMapWithSpecular(cliff_bump, 2.5f, 0.35f,
                size));
    }

    private static StructureLayers genSnow(int size, Channel noise8, Channel noise256) {
        Channel empty = new Channel(size, size).fill(1f);
        Channel snow_bump1 = noise8.brightness(0.75f);
        Channel snow_bump2 = noise256.brightness(0.25f);
        Layer snow = new Layer(empty.copy().fill(VIKING_SNOW_COLOR.r()), empty.copy().fill(VIKING_SNOW_COLOR.g()), empty
                .copy().fill(VIKING_SNOW_COLOR.b()));
        Channel bump = snow_bump1.channelAdd(snow_bump2);
        snow.bump(bump, size / 1024f, 0f, 0.1f, 1f, 1f, 1f, 0f, 0f, 0f);
        if (DEBUG) snow.saveAsPNG("structure_snow");

        return new StructureLayers(toLayerWithAlpha(snow, 0.05f), toNormalMapWithSpecular(bump, 0.2f, 0.8f, size));
    }

    private static StructureLayers genBlack() {
        return new StructureLayers(new Channel(1, 1).fill(0f).toLayer(), getFlatNormal(1));
    }

    private static StructureLayers genSeabottom(
            Terrain terrain, int size) {
        Color.Standard color = SEA_BOTTOM_COLOR.get(terrain);
        Layer bottom = new Layer(
                new Channel(size, size).fill(color.r()),
                new Channel(size, size).fill(color.g()),
                new Channel(size, size).fill(color.b()),
                new Channel(size, size).fill(0.2f) // Smooth (low detail)
        );
        return new StructureLayers(bottom, getFlatNormal(size));
    }

    private static StructureLayers genDetail(int size, float detail_alpha_value, int seed,
            Channel noise8) {
        Channel detail_noise = new Midpoint(size, 4, 0.4f, seed).toChannel();
        detail_noise.perturb(noise8.scale(size, size), 0.05f);
        Channel detail_grey = new Channel(size, size).fill(DETAIL_GREY);
        detail_grey.bump(detail_noise, size / 64f, 0f, 0f, 1f, 0f).dynamicRange();
        Channel detail_alpha = new Channel(size, size).fill(detail_alpha_value);
        Layer detail = new Layer(detail_grey, detail_grey, detail_grey, detail_alpha);
        if (DEBUG) detail.saveAsPNG("structure_detail");
        return new StructureLayers(detail, getFlatNormal(size));
    }

    private static Layer getFlatNormal(int size) {
        return new Layer(
                new Channel(size, size).fill(0.5f),
                new Channel(size, size).fill(0.5f),
                new Channel(size, size).fill(1.0f),
                new Channel(size, size).fill(0.0f) // Specular = 0
        );
    }

    private static void generateAlphas(Terrain terrain, int unitGridsPerWorld, int metersPerHeightUnit,
            int heightScale, float vegetationAmount, float accessThreshold, float buildThreshold,
            Channel height, Channel slope, Channel relheight, SceneLighting lighting, Channel[] alphaMaps) {
        int seed = DEFAULT_NOISE_SEED;
        Channel alpha0;
        Channel alpha1;
        Channel alpha2;
        Channel alpha3;

        switch (terrain) {
            case NATIVE -> {
                alpha0 = generateDirtAlpha(height, relheight);
                if (DEBUG) alpha0.toLayer().saveAsPNG("alpha_dirt");
                alpha1 = generateRubbleAlpha(height, slope, relheight, buildThreshold, accessThreshold);
                if (DEBUG) alpha1.toLayer().saveAsPNG("alpha_rubble");
                alpha2 = generateRockAlpha(slope, accessThreshold);
                if (DEBUG) alpha2.toLayer().saveAsPNG("alpha_rock");
                alpha3 = generateGrassAlpha(unitGridsPerWorld, seed, height, slope, relheight, vegetationAmount,
                        accessThreshold);
                if (DEBUG) alpha3.toLayer().saveAsPNG("alpha_grass");
                alphaMaps[0] = alpha0;
                alphaMaps[1] = alpha1;
                alphaMaps[2] = alpha2;
                alphaMaps[3] = alpha3;
            }
            case VIKING -> {
                alpha0 = generateSoilAlpha(height, relheight);
                if (DEBUG) alpha0.toLayer().saveAsPNG("alpha_soil");
                alpha1 = generateCliffAlpha(slope, accessThreshold);
                if (DEBUG) alpha1.toLayer().saveAsPNG("alpha_cliff");
                alpha2 = generateGrassAlpha(unitGridsPerWorld, seed, height, slope, relheight, vegetationAmount,
                        accessThreshold);
                if (DEBUG) alpha2.toLayer().saveAsPNG("alpha_grass");
                alpha3 = generateSnowAlpha(height, alpha1.copy());
                if (DEBUG) alpha3.toLayer().saveAsPNG("alpha_snow");
                alphaMaps[0] = alpha0;
                alphaMaps[1] = alpha1;
                alphaMaps[2] = alpha2;
                alphaMaps[3] = alpha3;
            }
        }

        Channel seabottomAlpha = generateSeabottomAlpha(terrain, height);
        if (DEBUG) seabottomAlpha.toLayer().saveAsPNG("alpha_seabottom");

        // Generate shadow and highlight alpha
        Channel shadow = new Channel(unitGridsPerWorld, unitGridsPerWorld);
        Channel highlight = new Channel(unitGridsPerWorld, unitGridsPerWorld);
        float lx = -lighting.lightDirection().x();
        float ly = -lighting.lightDirection().y();
        float lz = lighting.lightDirection().z();
        float threshold = lz;
        float nz = 2f * metersPerHeightUnit / heightScale;
        float nzlz = nz * lz;
        float nz2 = nz * nz;
        for (int y = 0; y < unitGridsPerWorld; y++) {
            for (int x = 0; x < unitGridsPerWorld; x++) {
                float nx = height.getPixelWrap(x + 1, y) - height.getPixelWrap(x - 1, y);
                float ny = height.getPixelWrap(x, y + 1) - height.getPixelWrap(x, y - 1);
                float light = (nx * lx + ny * ly + nzlz) / ((float) Math.sqrt(nx * nx + ny * ny + nz2));
                if (light > threshold) {
                    highlight.putPixel(x, y, light);
                    shadow.putPixel(x, y, threshold);
                } else {
                    highlight.putPixel(x, y, threshold);
                    shadow.putPixel(x, y, Math.max(0, light));
                }
            }
        }
        highlight.dynamicRange(0f, 0.25f);
        shadow.invert().dynamicRange(0f, 0.75f);
        ProgressListener.progress(1 / 14f);

        // Generate shadowcasting along Southwest-to-Northeast solar direction
        Channel shadowcast = new Channel(unitGridsPerWorld, unitGridsPerWorld);
        float descent = (8f / unitGridsPerWorld) * (lz / (lx * (float) Math.sqrt(2.0)));
        for (int start = 0; start < unitGridsPerWorld; start++) {
            float peak = 0;
            for (int x = start, y = 0; x < unitGridsPerWorld && y < unitGridsPerWorld; x++, y++) {
                float val = height.getPixel(x, y);
                peak = peak - descent;
                if (peak > val) {
                    shadowcast.putPixel(x, y, 1f);
                } else {
                    peak = val;
                }
            }
            if (start > 0) {
                peak = 0;
                for (int x = 0, y = start; x < unitGridsPerWorld && y < unitGridsPerWorld; x++, y++) {
                    float val = height.getPixel(x, y);
                    peak = peak - descent;
                    if (peak > val) {
                        shadowcast.putPixel(x, y, 1f);
                    } else {
                        peak = val;
                    }
                }
            }
        }
        shadow.channelBrightest(shadowcast.smooth(1).brightness(0.67f));
        if (DEBUG) shadow.toLayer().saveAsPNG("alpha_shadow");
        alphaMaps[4] = highlight;
        alphaMaps[5] = shadow;
        alphaMaps[6] = seabottomAlpha;
    }

    private static Channel generateDirtAlpha(Channel height, Channel relheight) {
        Channel dirtAlpha = height.copy().dynamicRange(1.1f * SimulationConfig.SEA_LEVEL, 2f
                * SimulationConfig.SEA_LEVEL, 0f, 1f);
        dirtAlpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.6f, 0f, 0.5f));
        return dirtAlpha;
    }

    private static Channel generateRubbleAlpha(Channel height, Channel slope, Channel relheight, float buildThreshold,
            float accessThreshold) {
        Channel rubbleAlpha = slope.copy().dynamicRange(buildThreshold, accessThreshold, 0f, 1f);
        rubbleAlpha.channelSubtract(height.copy().invert().dynamicRange(0.8f, 1f, 0f, 1f));
        rubbleAlpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.65f, 0f, 0.5f));
        return rubbleAlpha;
    }

    private static Channel generateRockAlpha(Channel slope, float accessThreshold) {
        return slope.copy().threshold(accessThreshold, 1f);
    }

    /**
     * Generates the grass probability alpha channel used for terrain blending and vegetation placement.
     */
    private static Channel generateGrassAlpha(int size, int seed, Channel height, Channel slope, Channel relheight,
            float vegetationAmount, float accessThreshold) {
        Channel grassAlpha = new Midpoint(size, 4, 0.45f, seed).toChannel()
                .dynamicRange(1.0f - vegetationAmount, 1.0f, 0.0f, 1.0f)
                .gamma2();
        grassAlpha.channelBrightest(slope.copy()
                .dynamicRange(0.0f, accessThreshold, 0.0f, 1.0f)
                .invert()
                .dynamicRange(1.0f - vegetationAmount, 1.0f, 0.0f, 1.0f)
                .gamma2());
        grassAlpha.channelAdd(relheight.copy().invert().add(-0.5f).multiply(2f));
        grassAlpha.channelSubtract(height.copy().invert().dynamicRange(0.6f, 0.8f, 0f, 1f));
        grassAlpha.channelSubtract(slope.copy().threshold(0.75f * accessThreshold, 1f).smooth(3));
        grassAlpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.7f, 0f, 0.5f));

        return grassAlpha;
    }

    private static Channel generateSoilAlpha(Channel height, Channel relheight) {
        Channel soilAlpha = height.copy().dynamicRange(1.1f * SimulationConfig.SEA_LEVEL, 2f
                * SimulationConfig.SEA_LEVEL, 0f, 1f);
        soilAlpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.6f, 0f, 0.5f));
        return soilAlpha;
    }

    private static Channel generateCliffAlpha(Channel slope, float accessThreshold) {
        return slope.copy().threshold(accessThreshold, 1f);
    }

    private static Channel generateSnowAlpha(Channel height, Channel cliffAlpha) {
        Channel snowAlpha = height.copy().dynamicRange(0.54f, 0.62f, 0f, 1f);
        snowAlpha.channelSubtract(cliffAlpha);
        snowAlpha.smooth(1).smooth(1);
        snowAlpha.channelMultiply(height.copy().dynamicRange(0.55f, 0.62f, 0f, 1f));
        return snowAlpha;
    }

    private static Channel generateSeabottomAlpha(Terrain terrain, Channel height) {
        Channel seabottomAlpha = height.copy().invert().dynamicRange(1f - SimulationConfig.SEA_LEVEL, 1f, 0f, 1f);
        return switch (terrain) {
            case NATIVE -> seabottomAlpha.grow(0f, 1).gamma(0.5f);
            case VIKING -> seabottomAlpha.gamma(0.5f);
        };
    }
}
