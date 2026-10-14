package com.oddlabs.tt.procedural.landscape;


import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Tools;
import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.procedural.noise.Midpoint;
import com.oddlabs.tt.procedural.noise.Voronoi;
import com.oddlabs.tt.procedural.shape.Hill;
import com.oddlabs.tt.simulation.SimulationConfig;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.Placement;
import com.oddlabs.tt.simulation.landscape.PlantLocation;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.SupplyPlacement;
import com.oddlabs.tt.simulation.landscape.TreePlacement;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Procedurally generates and manages terrain elevation, flora, and resource placements.
 */
public final class ProceduralLandscape implements Landscape {
    private static final boolean DEBUG = false;
    private static final int NUM_PLANT_TYPES = 4;
    private static final int VEGETATION_NOISE_SEED = 1;

    private record GenerationContext(
            Terrain terrain,
            int numPlayers,
            int metersPerWorld,
            int unitGridsPerWorld,
            int metersPerHeightUnit,
            int heightScale,
            float hills,
            float vegetationAmount,
            float suppliesAmount,
            int seed,
            float area,
            int maxTrees,
            int maxPalmtrees,
            int maxRock,
            int maxIron,
            int maxPlants,
            float accessThreshold,
            float buildThreshold,
            Random random) {
    }

    private record TerrainData(
            Channel height,
            Channel slope,
            Channel relheight,
            Channel access,
            byte[][] build,
            boolean[][] accessGrid) {
    }

    private record UnitLocations(List<List<SpawnLocation>> playerLocations, int[][] supplyLocations) {
    }

    private record PlacedSupplies(
            List<TreePlacement> trees,
            List<TreePlacement> palmtrees,
            List<SupplyPlacement> rock,
            List<SupplyPlacement> iron) {
    }

    private final IslandConfig config;
    private final float[] height;
    private final byte[][] build;
    private final boolean[][] access_grid;

    private final List<TreePlacement> trees;
    private final List<TreePlacement> palmtrees;
    private final List<SupplyPlacement> rock;
    private final List<SupplyPlacement> iron;

    private final float sea_level_meters;
    private final int height_scale;
    private final float access_threshold;
    private final float build_threshold;
    private final List<List<SpawnLocation>> player_locations;

    // generate plant maps
    @SuppressWarnings("unchecked")
    private final List<PlantLocation>[] plants = IntStream.range(0, NUM_PLANT_TYPES)
            .mapToObj(i -> new ArrayList<PlantLocation>())
            .toArray(List[]::new);

    public ProceduralLandscape(int num_players, IslandConfig config, int initial_unit_count, float random_start_pos) {
        this.config = config;
        Terrain terrain = config.terrain();
        float hills = (float) Math.sqrt(config.hills());
        float vegetation_amount = 0.25f + 0.75f * config.vegetation();
        float supplies_amount = 0.25f + 0.75f * config.supplies();
        int seed = (int) config.seed();
        int meters_per_world = config.metersPerWorld();
        int unit_grids_per_world = meters_per_world / HeightMap.METERS_PER_UNIT_GRID;
        int meters_per_height_unit = meters_per_world / unit_grids_per_world;
        int height_scale;
        float access_threshold;
        int size_multiplier;
        switch (meters_per_world) {
            case 256 -> {
                size_multiplier = 1;
                height_scale = 32;
                access_threshold = 0.05f;
            }
            case 512 -> {
                size_multiplier = 4;
                height_scale = 48;
                access_threshold = 0.0375f;
            }
            case 1024 -> {
                size_multiplier = 16;
                height_scale = 64;
                access_threshold = 0.025f;
            }
            default -> {
                size_multiplier = 0;
                height_scale = 0;
                access_threshold = 0f;
                assert false : "illegal meters_per_world";
            }
        }
        float build_threshold = access_threshold / 2f;
        this.height_scale = height_scale;
        this.access_threshold = access_threshold;
        this.build_threshold = build_threshold;
        this.sea_level_meters = height_scale * SimulationConfig.SEA_LEVEL;
        Random random = new Random(seed);

        float area = size_multiplier * 10000f;
        int max_plants = size_multiplier * 64;

        int max_trees = switch (terrain) {
            case NATIVE -> (int) Math.pow(2, 2 * Utils.powerOf2Log2(meters_per_world) - 9);
            case VIKING -> (int) (.75f * Math.pow(2, 2 * Utils.powerOf2Log2(meters_per_world) - 9));
        };
        int max_palmtrees = switch (terrain) {
            case NATIVE -> max_trees >> 1;
            case VIKING -> max_trees;
        };

        int max_rock = max_trees >> 3;
        int max_iron = max_trees >> 4;

        GenerationContext ctx = new GenerationContext(
                terrain,
                num_players,
                meters_per_world,
                unit_grids_per_world,
                meters_per_height_unit,
                height_scale,
                hills,
                vegetation_amount,
                supplies_amount,
                seed,
                area,
                max_trees,
                max_palmtrees,
                max_rock,
                max_iron,
                max_plants,
                access_threshold,
                build_threshold,
                random
        );

        TerrainData terrainData = switch (terrain) {
            case NATIVE -> generateTerrainNative(ctx);
            case VIKING -> generateTerrainViking(ctx);
        };
        Channel heightMap = terrainData.height();
        Channel slope = terrainData.slope();
        Channel relheight = terrainData.relheight();
        Channel access = terrainData.access();
        this.build = terrainData.build();
        this.access_grid = terrainData.accessGrid();

        if (DEBUG) heightMap.toLayer().saveAsPNG("height");
        ProgressListener.progress();
        Channel grass_alpha = generateGrassAlpha(ctx.unitGridsPerWorld,
                VEGETATION_NOISE_SEED, heightMap, slope, relheight, ctx.vegetationAmount, ctx.accessThreshold);
        ProgressListener.progress();
        UnitLocations unitLocations = generateUnitLocations(ctx, initial_unit_count, random_start_pos, access,
                this.build);
        this.player_locations = unitLocations.playerLocations();
        PlacedSupplies placedSupplies = generateSupplies(ctx, grass_alpha, heightMap, slope, relheight, access,
                unitLocations.supplyLocations());
        this.trees = placedSupplies.trees();
        this.palmtrees = placedSupplies.palmtrees();
        this.rock = placedSupplies.rock();
        this.iron = placedSupplies.iron();

        // scale height map vertically
        for (int y = 0; y < unit_grids_per_world; y++) {
            for (int x = 0; x < unit_grids_per_world; x++) {
                heightMap.putPixel(x, y, height_scale * heightMap.getPixel(x, y));
            }
        }
        this.height = heightMap.getPixels();

        if (DEBUG) access.toLayer().saveAsPNG("access_connected");
    }


    // ***********
    // * TERRAIN *
    // ***********
    private TerrainData generateTerrainNative(GenerationContext ctx) {
        int unit_grids_per_world = ctx.unitGridsPerWorld;
        // generate height map
        Channel heightMap = new Mountain(unit_grids_per_world, Utils.powerOf2Log2(unit_grids_per_world) - 6, 0.5f,
                ctx.seed)
                .toChannel().multiply(0.67f);
        Voronoi voronoi = new Voronoi(unit_grids_per_world, 4, 4, 1, 1f, ctx.seed);
        Channel cliffs = voronoi.getDistance(-1f, 1f, 0f).brightness(1.5f).multiply(0.33f);
        heightMap.channelAdd(cliffs);

        // Fist of God (tm)
        if (unit_grids_per_world > 128) {
            heightMap.channelSubtract(voronoi.getDistance(1f, 0f, 0f).gamma(.5f).flipV().rotate(90));
        } else {
            heightMap.channelSubtract(voronoi.getDistance(-1f, 1f, 0f).gamma(.5f).flipV().rotate(90));
        }

        heightMap.perturb(new Midpoint(unit_grids_per_world, 2, 0.5f, ctx.seed).toChannel(), 0.25f);
        Channel shape = new Hill(unit_grids_per_world, Hill.Shape.OVAL).toChannel();
        heightMap.channelAdd(shape.copy().multiply(0.15f));
        heightMap.channelSubtract(shape.copy().invert().multiply(0.5f));
        heightMap.erode((24f - ctx.hills * 12f) / unit_grids_per_world, unit_grids_per_world >> 2);
        heightMap.channelMultiply(shape.gamma2());
        heightMap.smooth(1);
        heightMap = beaches(heightMap);

        // fix edges
        for (int y = 0; y < unit_grids_per_world; y += (unit_grids_per_world - 1)) {
            for (int x = 0; x < unit_grids_per_world; x++) {
                heightMap.putPixel(x, y, 0f);
            }
        }
        for (int y = 1; y < unit_grids_per_world - 1; y++) {
            for (int x = 0; x < unit_grids_per_world; x += (unit_grids_per_world - 1)) {
                heightMap.putPixel(x, y, 0f);
            }
        }

        Channel slope = heightMap.copy().lineart();
        if (DEBUG) slope.copy().dynamicRange().toLayer().saveAsPNG("slope");
        Channel relheight = heightMap.copy().relativeIntensityNormalized(Math.max(1, unit_grids_per_world >> 5));
        if (DEBUG) relheight.toLayer().saveAsPNG("relheight");
        Channel access = generateThresholdMap(heightMap, slope, ctx.accessThreshold, unit_grids_per_world)
                .largestConnected(1f);
        boolean[][] accessGrid = createAccessGrid(access);
        if (DEBUG) access.toLayer().saveAsPNG("access");
        byte[][] buildGrid = generateBuildMap(generateThresholdMap(heightMap, slope, ctx.buildThreshold,
                unit_grids_per_world).channelMultiply(access));
        return new TerrainData(heightMap, slope, relheight, access, buildGrid, accessGrid);
    }

    private TerrainData generateTerrainViking(GenerationContext ctx) {
        int unit_grids_per_world = ctx.unitGridsPerWorld;
        // generate height map
        Channel heightMap = new Mountain(unit_grids_per_world, Utils.powerOf2Log2(unit_grids_per_world) - 6, 0.5f,
                ctx.seed)
                .toChannel().add(.5f).dynamicRange().gamma2().multiply(0.67f);

        Voronoi voronoi = new Voronoi(unit_grids_per_world, 8, 8, 1, 1f, ctx.seed, true);
        Channel cliffs = voronoi.getDistance(-1f, 1f, 0f).brightness(1.25f).multiply(0.33f);
        heightMap.channelAdd(cliffs).dynamicRange();

        // Fist of God™
        Voronoi voronoi2 = new Voronoi(unit_grids_per_world, 4, 4, 1, 1f, ctx.seed);
        if (unit_grids_per_world > 128) {
            heightMap.channelSubtract(voronoi2.getDistance(1f, 0f, 0f).gamma(.5f).multiply(.5f));
        } else {
            heightMap.channelSubtract(voronoi2.getDistance(-1f, 1f, 0f).gamma(.5f).multiply(.5f));
        }

        Channel hitpoint = voronoi.getHitpoint().smooth(1);
        Channel hitpoint2 = hitpoint.copy().erodeThermal(4f / unit_grids_per_world, unit_grids_per_world >> 3);
        Channel noise = new Midpoint(unit_grids_per_world, 3, 0.25f, ctx.seed).toChannel().threshold(0.75f * ctx.hills,
                1f);
        Channel heightcut = hitpoint.channelMultiply(noise.copy().invert()).channelAdd(hitpoint2.copy().channelMultiply(
                noise));
        heightMap.channelMultiply(heightcut);
        heightMap.perturb(new Midpoint(unit_grids_per_world, 2, 0.5f, ctx.seed).toChannel(), 0.25f);
        heightMap.erode((24f - ctx.hills * 12f) / unit_grids_per_world, unit_grids_per_world >> 2);

        Channel shape = new Hill(unit_grids_per_world, Hill.Shape.SQUARE).toChannel().smoothGain().gamma8();
        heightMap.channelMultiply(shape);

        // add roughness to inaccessible areas
        Channel slope = heightMap.copy().lineart();
        Channel peakarea = slope.threshold(0f, ctx.accessThreshold).largestConnected(1f).invert().channelMultiply(
                hitpoint2);
        Channel peaks = new Midpoint(unit_grids_per_world, 4, 0.75f, 42).toChannel().channelMultiply(peakarea).multiply(
                .1f);
        heightMap.channelAdd(peaks);
        heightMap.smooth(1);

        // fix edges
        for (int y = 0; y < unit_grids_per_world; y += (unit_grids_per_world - 1)) {
            for (int x = 0; x < unit_grids_per_world; x++) {
                heightMap.putPixel(x, y, 0f);
            }
        }
        for (int y = 1; y < unit_grids_per_world - 1; y++) {
            for (int x = 0; x < unit_grids_per_world; x += (unit_grids_per_world - 1)) {
                heightMap.putPixel(x, y, 0f);
            }
        }

        slope = heightMap.copy().lineart();
        if (DEBUG) slope.copy().dynamicRange().toLayer().saveAsPNG("slope");
        Channel relheight = heightMap.copy().relativeIntensityNormalized(Math.max(1, unit_grids_per_world >> 5));
        if (DEBUG) relheight.toLayer().saveAsPNG("relheight");
        Channel access = generateThresholdMap(heightMap, slope, ctx.accessThreshold, unit_grids_per_world)
                .largestConnected(1f);
        boolean[][] accessGrid = createAccessGrid(access);
        if (DEBUG) access.toLayer().saveAsPNG("access");
        byte[][] buildGrid = generateBuildMap(generateThresholdMap(heightMap, slope, ctx.buildThreshold,
                unit_grids_per_world).channelMultiply(access));
        return new TerrainData(heightMap, slope, relheight, access, buildGrid, accessGrid);
    }

    private static boolean[][] createAccessGrid(Channel access) {
        int size = access.getWidth();
        boolean[][] grid = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                grid[y][x] = access.getPixel(x, y) > 0;
            }
        }
        return grid;
    }

    // shape beaches
    private static Channel beaches(Channel channel) {
        float sealevel = 1.1f * SimulationConfig.SEA_LEVEL;
        float threshold = 2f * sealevel;
        for (int y = 0; y < channel.height; y++) {
            for (int x = 0; x < channel.width; x++) {
                float value = channel.getPixel(x, y);
                if (value < sealevel) {
                    value = Tools.interpolateSmooth(0, sealevel, value / sealevel);
                    channel.putPixel(x, y, value);
                } else if (value < threshold) {
                    value = Tools.interpolateSmooth(sealevel, 2f * threshold - sealevel, 0.5f * (value - sealevel)
                            / (threshold - sealevel));
                    channel.putPixel(x, y, value);
                }
            }
        }
        return channel;
    }

    // generate threshold map
    private static Channel generateThresholdMap(Channel heightMap, Channel slopemap, float threshold,
            int unit_grids_per_world) {
        Channel channel = slopemap.copy().threshold(0f, threshold).channelSubtract(heightMap.copy().threshold(0f,
                SimulationConfig.SEA_LEVEL));
        // fix edges
        for (int y = 0; y < unit_grids_per_world; y += (unit_grids_per_world - 1)) {
            for (int x = 0; x < unit_grids_per_world; x++) {
                channel.putPixel(x, y, 0f);
            }
        }
        for (int y = 1; y < unit_grids_per_world - 1; y++) {
            for (int x = 0; x < unit_grids_per_world; x += (unit_grids_per_world - 1)) {
                channel.putPixel(x, y, 0f);
            }
        }
        return channel;
    }

    // generate build map
    private static byte[][] generateBuildMap(Channel thresholdmap) {
        if (DEBUG) thresholdmap.toLayer().saveAsPNG("build_tresholdmap");
        int size = thresholdmap.getWidth();
        boolean[][] build_grid = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                build_grid[y][x] = thresholdmap.getPixel(x, y) > 0;
            }
        }
        byte[][] byte_grid = new byte[build_grid.length][build_grid[0].length];
        byte max = (byte) RaceData.MAX_BUILDING_SIZE;
        for (byte i = 0; i < max; i++) {
            for (int y = 1; y < build_grid.length - 1; y++) {
                for (int x = 1; x < build_grid[y].length - 1; x++) {
                    if (!build_grid[y][x] && byte_grid[y][x] == i) {
                        for (int k = -1; k <= 1; k++) {
                            for (int l = -1; l <= 1; l++) {
                                if (build_grid[y + k][x + l]) {
                                    build_grid[y + k][x + l] = false;
                                    byte_grid[y + k][x + l] = (byte) (i + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int y = 1; y < build_grid.length - 1; y++) {
            for (int x = 1; x < build_grid[y].length - 1; x++) {
                if (build_grid[y][x])
                    byte_grid[y][x] = max;
            }
        }
        return byte_grid;
    }


    private static Channel generateGrassAlpha(int size, int seed, Channel height, Channel slope, Channel relheight,
            float vegetation_amount, float access_threshold) {
        Channel grass_alpha = new Midpoint(size, 4, 0.45f, seed).toChannel()
                .dynamicRange(1.0f - vegetation_amount, 1.0f, 0.0f, 1.0f)
                .gamma2();
        grass_alpha.channelBrightest(slope.copy()
                .dynamicRange(0.0f, access_threshold, 0.0f, 1.0f)
                .invert()
                .dynamicRange(1.0f - vegetation_amount, 1.0f, 0.0f, 1.0f)
                .gamma2());
        grass_alpha.channelAdd(relheight.copy().invert().add(-0.5f).multiply(2f));
        grass_alpha.channelSubtract(height.copy().invert().dynamicRange(0.6f, 0.8f, 0f, 1f));
        grass_alpha.channelSubtract(slope.copy().threshold(0.75f * access_threshold, 1f).smooth(3));
        grass_alpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.7f, 0f, 0.5f));

        return grass_alpha;
    }

    // ************
    // * SUPPLIES *
    // ************
    private PlacedSupplies generateSupplies(GenerationContext ctx, Channel grass_alpha, Channel height, Channel slope,
            Channel relheight, Channel access, int[][] supply_locations) {
        // generate overall probability map for rock and iron resources
        Channel centerprob = new Hill(ctx.unitGridsPerWorld, Hill.Shape.CIRCLE).toChannel().addClip(-.5f)
                .dynamicRange();

        // generate (oak)tree/palmtree(/pine) maps
        Channel noise = new Midpoint(ctx.unitGridsPerWorld, Utils.powerOf2Log2(ctx.unitGridsPerWorld) - 3, 0.33f,
                ctx.seed)
                .toChannel();
        Channel tree_channel = grass_alpha.copy();
        Channel palmtree_channel = height.copy();
        ProgressListener.progress(1 / 14f);

        switch (ctx.terrain) {
            case NATIVE -> {
                tree_channel.threshold(0.5f, 1f);
                tree_channel.channelAdd(noise.rotate(90).copy().threshold(0.9f, 1f));
                tree_channel.channelSubtract(slope.copy().threshold(ctx.accessThreshold, 1f));
                tree_channel.channelSubtract(noise.copy().dynamicRange(0.1f, 1f));

                palmtree_channel.invert().dynamicRange(0.6f, 0.89f, 0f, 1f);
                palmtree_channel.channelSubtract(height.copy().invert().dynamicRange(0.89f, 0.9f, 0f, 1f));
                palmtree_channel.channelSubtract(noise.rotate(90)).channelAdd(noise.rotate(90).copy().threshold(0.9f,
                        1f)).channelSubtract(slope.copy().threshold(ctx.accessThreshold, 1f));
            }
            case VIKING -> {
                tree_channel.gamma8();
                tree_channel.channelMultiply(height.copy().dynamicRange(0.55f, 0.65f, 1f, 0f));
                tree_channel.channelSubtract(slope.copy().threshold(ctx.accessThreshold, 1f));
                tree_channel.channelSubtract(noise.copy());

                palmtree_channel = grass_alpha.copy().channelMultiply(height.copy().dynamicRange(0.5f, 0.6f, 1f, 0f))
                        .invert();
                palmtree_channel.channelSubtract(height.copy().invert().dynamicRange(0.8f, 0.875f, 0f, 1f));
                palmtree_channel.channelSubtract(slope.copy().threshold(ctx.accessThreshold, 1f));
                palmtree_channel.channelSubtract(noise.rotate(90));
            }
        }

        if (DEBUG) tree_channel.toLayer().saveAsPNG("supplies_trees");
        if (DEBUG) palmtree_channel.toLayer().saveAsPNG("supplies_palmtrees");
        ProgressListener.progress(1 / 14f);

        // generate rock and iron supplies map
        Channel rock_channel = relheight.copy();

        switch (ctx.terrain) {
            case NATIVE -> rock_channel
                    .invert()
                    .threshold(0.5f, 1f)
                    .channelMultiply(noise.rotate(90).copy().gamma8().invert())
                    .channelMultiply(centerprob);
            case VIKING -> rock_channel
                    .threshold(0f, 0.5f)
                    .channelMultiply(noise.rotate(90).copy().gamma8().invert());
        }

        Channel iron_channel = rock_channel.copy().rotate(90).flipV();
        if (DEBUG) rock_channel.toLayer().saveAsPNG("supplies_rocks");
        if (DEBUG) iron_channel.toLayer().saveAsPNG("supplies_iron");

        Channel supplies = access.copy();
        float accessible = supplies.sum();

        // place trees
        Channel placedTrees = placeSupplies(tree_channel, supplies, 64, (int) (ctx.vegetationAmount * ctx.maxTrees
                * (accessible / ctx.area)));
        access.channelSubtract(placedTrees);
        if (DEBUG) placedTrees.toLayer().saveAsPNG("supplies_trees_placed");

        // place palmtrees
        Channel placedPalmtrees = placeSupplies(palmtree_channel, supplies, 64, (int) (ctx.vegetationAmount
                * ctx.maxPalmtrees * (accessible
                        / ctx.area)));
        access.channelSubtract(placedPalmtrees);
        if (DEBUG) placedPalmtrees.toLayer().saveAsPNG("supplies_palmtrees_placed");

        // place rock
        Channel placedRock = placeSupplies(rock_channel, supplies, 64, (int) (ctx.suppliesAmount * ctx.maxRock));
        access.channelSubtract(placedRock);
        if (DEBUG) placedRock.toLayer().saveAsPNG("supplies_rock_placed");

        // place iron
        Channel placedIron = placeSupplies(iron_channel, supplies, 64, (int) (ctx.suppliesAmount * ctx.maxIron));
        access.channelSubtract(placedIron);
        if (DEBUG) placedIron.toLayer().saveAsPNG("supplies_iron_placed");

        if (DEBUG) {
            IO.println("Number of trees placed: " + placedTrees.count(1f));
            IO.println("Number of palmtrees placed: " + placedPalmtrees.count(1f));
            IO.println("Number of rocks placed: " + placedRock.count(1f));
            IO.println("Number of iron ore placed: " + placedIron.count(1f));
        }

        // place extra supplies around starting locations
        int num_rock = 2;
        int num_iron = 1;
        for (int p = 0; p < ctx.numPlayers; p++) {
            for (int r = 0; r < num_rock; r++) {
                int[] location = access.find((ctx.unitGridsPerWorld >> 1), supply_locations[p][0],
                        supply_locations[p][1], 1f);
                placedRock.putPixel(location[0], location[1], 1f);
                access.putPixel(location[0], location[1], 0f);
            }
            for (int i = 0; i < num_iron; i++) {
                int[] location = access.find((ctx.unitGridsPerWorld >> 1), supply_locations[p][0],
                        supply_locations[p][1], 1f);
                placedIron.putPixel(location[0], location[1], 1f);
                access.putPixel(location[0], location[1], 0f);
            }
        }

        PlacedSupplies placedSupplies = new PlacedSupplies(
                getPositions(placedTrees, TreePlacement::new),
                getPositions(placedPalmtrees, TreePlacement::new),
                getPositions(placedRock, SupplyPlacement::new),
                getPositions(placedIron, SupplyPlacement::new));

        ProgressListener.progress(1 / 14f);

        tree_channel.channelBrightest(palmtree_channel).brightness(.5f);
        noise.scaleFast(noise.width >> 1, noise.height >> 1).tileDouble();

        Channel plantsmap = grass_alpha.copy();
        plantsmap.multiply(0.25f).add(0.75f);
        plantsmap.channelMultiply(slope.copy().threshold(0f, ctx.accessThreshold));
        plantsmap.channelMultiply(height.copy().threshold(SimulationConfig.SEA_LEVEL, 1f));

        Channel plants1 = plantsmap.copy().channelMultiply(noise.copy());
        Channel plants2 = plantsmap.copy().channelMultiply(noise.rotate(90));
        Channel plants3 = plantsmap.copy().channelMultiply(noise.rotate(90));
        Channel plants4 = plantsmap.copy().channelMultiply(noise.rotate(90));
        Channel place = new Channel(ctx.unitGridsPerWorld, ctx.unitGridsPerWorld);
        place = placePlants(ctx, plants1, place, 64, ctx.maxPlants >> 2, 0);
        place = placePlants(ctx, plants2, place, 64, ctx.maxPlants >> 2, 1);
        place = placePlants(ctx, plants3, place, 64, ctx.maxPlants >> 2, 2);
        place = placePlants(ctx, plants4, place, 64, ctx.maxPlants >> 2, 3);
        return placedSupplies;
    }

    // place supplies on map
    private static Channel placeSupplies(Channel probability, Channel supplies, int intervals,
            int max_count) {
        max_count = Math.min(probability.width * probability.height, max_count);
        int i = 0;
        float interval_size = 1f / intervals;
        float upper_bound = 1f;
        float lower_bound = upper_bound - interval_size;
        Channel place = new Channel(probability.width, probability.height);

        // place supplies
        out:
        while (i < max_count && lower_bound > interval_size) {
            for (int y = 1; y < probability.height - 1; y++) {
                for (int x = 1; x < probability.width - 1; x++) {
                    float val = probability.getPixel(x, y);
                    if (val <= upper_bound && val > lower_bound) {
                        // place resource if grid and its 4 neighbours are unoccupied
                        if (supplies.getPixel(x, y) > 0
                                && supplies.getPixel(x - 1, y) > 0
                                && supplies.getPixel(x + 1, y) > 0
                                && supplies.getPixel(x, y - 1) > 0
                                && supplies.getPixel(x, y - 1) > 0 // DO NOT FIX: This is a bug in the original game.
                        // fixing it alters resource placement and gameplay.
                        ) {
                            place.putPixel(x, y, 1f);
                            // make node neighbourhood inaccessible
                            for (int k = -1; k <= 1; k++) {
                                for (int l = -1; l <= 1; l++) {
                                    supplies.putPixelWrap(x + k, y + l, 0f);
                                }
                            }

                            i++;
                            if (i >= max_count) break out;
                        }
                    }
                }
            }
            lower_bound -= interval_size;
            upper_bound -= interval_size;
        }
        return place;
    }

    // place plants on map
    private Channel placePlants(GenerationContext ctx, Channel probability, Channel place, int intervals,
            int max_count, int plant_type) {
        max_count = Math.min(probability.width * probability.height, max_count);
        int i = 0;
        float interval_size = 1f / intervals;
        float upper_bound = 1f;
        float lower_bound = upper_bound - interval_size;

        // place plants
        out:
        while (i < max_count && lower_bound > interval_size) {
            for (int y = 2; y < probability.height - 2; y++) {
                for (int x = 2; x < probability.width - 2; x++) {
                    float val = probability.getPixel(x, y);
                    if (val <= upper_bound && val > lower_bound && place.getPixel(x, y) < 2 && ctx.random.nextFloat()
                            < .25) {
                        // place plant
                        plants[plant_type].add(new PlantLocation(
                                ctx.metersPerHeightUnit * (x + ctx.random.nextFloat()),
                                ctx.metersPerHeightUnit * (y + ctx.random.nextFloat())));

                        place.putPixelWrap(x, y, place.getPixel(x, y) + 1f);

                        i++;
                        if (i >= max_count) break out;
                    }
                }
            }
            lower_bound -= interval_size;
            upper_bound -= interval_size;
        }
        return place;
    }


    // ******************
    // * UNIT LOCATIONS *
    // ******************
    private static UnitLocations generateUnitLocations(GenerationContext ctx, int initial_unit_count,
            float random_start_pos,
            Channel access, byte[][] build) {
        Channel buildmap = new Channel(access.width, access.height);
        Channel buildmap_debug = DEBUG ? new Channel(access.width, access.height) : null;
        for (int y = 0; y < access.height; y++) {
            for (int x = 0; x < access.width; x++) {
                if (build[y][x] == RaceData.QUARTERS_SIZE) {
                    buildmap.putPixel(x, y, 1f);
                }
                if (DEBUG && buildmap_debug != null) buildmap_debug.putPixel(x, y, .2f * build[y][x]);
            }
        }
        if (DEBUG) buildmap.toLayer().saveAsPNG("buildmap");
        if (DEBUG && buildmap_debug != null) buildmap_debug.toLayer().saveAsPNG("buildmap_debug");
        // find initial starting locations
        List<List<SpawnLocation>> playerLocations = new ArrayList<>(ctx.numPlayers);
        int[][] supplyLocations = new int[ctx.numPlayers][2];
        float angle = 0.5f * (float) Math.PI;
        angle += random_start_pos * (float) Math.PI * 2; // random start for multiplayer games
        float angle_step = 2f * (float) Math.PI / ctx.numPlayers;
        float radius = 0.35f * ctx.unitGridsPerWorld;
        int scale = ctx.metersPerWorld / ctx.unitGridsPerWorld;
        int[] location_quarters;
        int[] location_armory;
        for (int i = 0; i < ctx.numPlayers; i++) {
            int x = (int) (radius * (float) Math.cos(angle) + (ctx.unitGridsPerWorld >> 1) + 0.5f);
            int y = (int) (radius * (float) Math.sin(angle) + (ctx.unitGridsPerWorld >> 1) + 0.5f);
            angle += angle_step;
            location_quarters = buildmap.findNoWrap((ctx.unitGridsPerWorld >> 1), x, y, 1f);
            for (int k = -RaceData.QUARTERS_SIZE; k <= RaceData.QUARTERS_SIZE; k++) {
                for (int l = -RaceData.QUARTERS_SIZE; l <= RaceData.QUARTERS_SIZE; l++) {
                    access.putPixelWrap(location_quarters[0] + k, location_quarters[1] + l, 0f);
                    buildmap.putPixelWrap(location_quarters[0] + k, location_quarters[1] + l, 0f);
                }
            }
            location_armory = buildmap.find((ctx.unitGridsPerWorld >> 1), location_quarters[0], location_quarters[1],
                    1f);
            for (int k = -RaceData.ARMORY_SIZE; k <= RaceData.ARMORY_SIZE; k++) {
                for (int l = -RaceData.ARMORY_SIZE; l <= RaceData.ARMORY_SIZE; l++) {
                    access.putPixelWrap(location_armory[0] + k, location_armory[1] + l, 0f);
                    buildmap.putPixelWrap(location_armory[0] + k, location_armory[1] + l, 0f);
                }
            }
            int[] location_unit_start = access.find((ctx.unitGridsPerWorld >> 1), location_quarters[0],
                    location_quarters[1], 1f);
            supplyLocations[i][0] = location_armory[0];
            supplyLocations[i][1] = location_armory[1];
            List<SpawnLocation> playerSpawnLocations = new ArrayList<>(initial_unit_count);
            for (int u = 0; u < initial_unit_count; u++) {
                int[] location_unit = access.find((ctx.unitGridsPerWorld >> 1), location_unit_start[0],
                        location_unit_start[1],
                        1f);
                access.putPixelWrap(location_unit[0], location_unit[1], 0f);
                playerSpawnLocations.add(new SpawnLocation(location_unit[0] * scale, location_unit[1] * scale));
            }
            playerLocations.add(playerSpawnLocations);
        }

        // shuffle player starting locations
        Collections.shuffle(playerLocations, ctx.random);
        return new UnitLocations(playerLocations, supplyLocations);
    }


    // ***************
    // * GET METHODS *
    // ***************


    @Override
    public IslandConfig config() {
        return config;
    }

    @Override
    public float[] heightmap() {
        return height;
    }

    @Override
    public boolean[][] accessGrid() {
        return access_grid;
    }

    @Override
    public byte[][] buildGrid() {
        return build;
    }

    @Override
    public List<TreePlacement> trees() {
        return trees;
    }

    @Override
    public List<TreePlacement> palmTrees() {
        return palmtrees;
    }

    @Override
    public List<SupplyPlacement> rocks() {
        return rock;
    }

    @Override
    public List<SupplyPlacement> iron() {
        return iron;
    }

    private static <P extends Placement> List<P> getPositions(Channel channel, Channel.BiIntFunction<P> function) {
        int count = channel.count(1f);
        List<P> list = new ArrayList<>(count);
        channel.matching(p -> p == 1f, (x, y) -> list.add(function.apply(x, y)));
        return list;
    }

    @Override
    public List<PlantLocation>[] plants() {
        return plants;
    }

    @Override
    public List<List<SpawnLocation>> startingLocations() {
        return player_locations;
    }

    @Override
    public float seaLevelMeters() {
        return sea_level_meters;
    }

    @Override
    public int heightScale() {
        return height_scale;
    }

    @Override
    public float accessThreshold() {
        return access_threshold;
    }

    @Override
    public float buildThreshold() {
        return build_threshold;
    }
}
