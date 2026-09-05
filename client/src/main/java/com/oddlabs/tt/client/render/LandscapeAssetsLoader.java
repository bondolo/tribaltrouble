package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.simulation.model.Terrain;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.stream.IntStream;

/**
 * Loads landscape-associated sprites (rocks, iron, plants, chickens) into the asset registry.
 */
public final class LandscapeAssetsLoader {
    public static final int SUPPLY_FRAGMENT_COUNT = 5;

    public LandscapeAssetsLoader(RenderQueues queues) {
        int num_progress = 13;
        ProgressListener.progress(10f / num_progress);

        var fragments = IntStream.rangeClosed(1, SUPPLY_FRAGMENT_COUNT)
                .mapToObj(i -> String.format("/geometry/misc/rock_%d.binsprite", i))
                .map(rsrc -> new SpriteFile(rsrc, RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false))
                .toArray(SpriteFile[]::new);

        SpriteKey[] rock_fragment_sprites = Arrays.stream(fragments)
                .map(queues::register)
                .toArray(SpriteKey[]::new);

        SpriteKey[] iron_fragment_sprites = Arrays.stream(fragments)
                .map(spriteFile -> queues.register(spriteFile, 1))
                .toArray(SpriteKey[]::new);
        ProgressListener.progress(1f / num_progress);

        EnumMap<Terrain, SpriteKey[]> plant_sprites = new EnumMap<>(Terrain.class);
        plant_sprites.put(
                Terrain.NATIVE, IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> String.format("/geometry/misc/plant_%d.binsprite", i))
                        .map(rsrc -> new SpriteFile(rsrc, RenderConfig.NO_MIPMAP_CUTOFF, true, false, true, true, true))
                        .map(queues::register)
                        .toArray(SpriteKey[]::new));
        plant_sprites.put(
                Terrain.VIKING, IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> String.format("/geometry/misc/viking_plant_%d.binsprite", i))
                        .map(rsrc -> new SpriteFile(rsrc, RenderConfig.NO_MIPMAP_CUTOFF, true, false, true, true, true))
                        .map(queues::register)
                        .toArray(SpriteKey[]::new));
        ProgressListener.progress(1f / num_progress);

        SpriteFile sprite_list_chicken = new SpriteFile("/geometry/misc/chicken.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteKey chicken = queues.register(sprite_list_chicken);

        AssetRegistry ar = AssetRegistry.getInstance();
        ar.registerRockFragments(rock_fragment_sprites);
        ar.registerIronFragments(iron_fragment_sprites);
        for (var entry : plant_sprites.entrySet()) {
            ar.registerPlants(entry.getKey(), entry.getValue());
        }
        ar.registerChicken(chicken);

        ProgressListener.progress(1f / num_progress);
    }
}
