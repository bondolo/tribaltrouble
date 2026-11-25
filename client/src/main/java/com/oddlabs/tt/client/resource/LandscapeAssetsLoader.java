package com.oddlabs.tt.client.resource;

import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.client.render.SelectableShadowRenderer;
import com.oddlabs.tt.engine.procedural.GeneratorHalos;
import com.oddlabs.tt.engine.render.DecalRenderer;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.ShadowListKey;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.simulation.model.Terrain;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.IntStream;

/// Loads landscape-associated sprites (rocks, iron, plants, chickens) for an {@link AssetRegistry}.
public final class LandscapeAssetsLoader {
  public static final int SUPPLY_FRAGMENT_COUNT = 5;

  private static final GeneratorHalos DEFAULT_SHADOW_DESC =
      new GeneratorHalos(
          DecalRenderer.HALO_LUT_RESOLUTION,
          new float[][] {{0f, 0.75f}, {0.5f, 0f}},
          new float[][] {{0.40f, 0f}, {0.41f, 1f}, {0.48f, 1f}, {0.49f, 0f}});

  private LandscapeAssetsLoader() {}

  public static AssetRegistry.LandscapeAssets load(RenderQueues queues) {
    int num_progress = 13;
    ProgressListener.progress(10f / num_progress);

    var fragments =
        IntStream.rangeClosed(1, SUPPLY_FRAGMENT_COUNT)
            .mapToObj(i -> String.format("/geometry/misc/rock_%d.binsprite", i))
            .map(rsrc -> new SpriteFile(rsrc, RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false))
            .toArray(SpriteFile[]::new);

    List<SpriteKey> rockFragmentSprites = Arrays.stream(fragments).map(queues::register).toList();

    List<SpriteKey> ironFragmentSprites =
        Arrays.stream(fragments).map(spriteFile -> queues.register(spriteFile, 1)).toList();
    ProgressListener.progress(1f / num_progress);

    EnumMap<Terrain, List<SpriteKey>> plantSprites = new EnumMap<>(Terrain.class);
    plantSprites.put(
        Terrain.NATIVE,
        IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/geometry/misc/plant_%d.binsprite", i))
            .map(rsrc -> new SpriteFile(rsrc, RenderConfig.NO_MIPMAP_CUTOFF, false, false, true, false, true))
            .map(queues::register)
            .toList());
    plantSprites.put(
        Terrain.VIKING,
        IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/geometry/misc/viking_plant_%d.binsprite", i))
            .map(rsrc -> new SpriteFile(rsrc, RenderConfig.NO_MIPMAP_CUTOFF, false, false, true, false, true))
            .map(queues::register)
            .toList());
    ProgressListener.progress(1f / num_progress);

    SpriteFile sprite_list_chicken =
        new SpriteFile("/geometry/misc/chicken.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    SpriteKey chicken = queues.register(sprite_list_chicken);
    ShadowListKey<SelectableShadowRenderer> defaultShadow =
        queues.registerShadowRenderer(
            DEFAULT_SHADOW_DESC,
            SelectableShadowRenderer.class,
            () -> new SelectableShadowRenderer(DEFAULT_SHADOW_DESC));

    ProgressListener.progress(1f / num_progress);
    return new AssetRegistry.LandscapeAssets(
        rockFragmentSprites, ironFragmentSprites, plantSprites, chicken, defaultShadow);
  }
}
