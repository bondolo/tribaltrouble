package com.oddlabs.tt.client.resource;

import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.client.render.SelectableShadowRenderer;
import com.oddlabs.tt.engine.font.ColorGraphemeGenerator;
import com.oddlabs.tt.engine.procedural.GeneratorHalos;
import com.oddlabs.tt.engine.procedural.GeneratorLightning;
import com.oddlabs.tt.engine.procedural.GeneratorPoison;
import com.oddlabs.tt.engine.procedural.GeneratorSmoke;
import com.oddlabs.tt.engine.render.DecalRenderer;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.ShadowListKey;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.engine.resource.TextureFile;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.WeaponTier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

/// Loader that registers client-side race, effect, and emoji graphics assets and returns immutable asset bundles.
public final class RacesAssetsLoader {
  private static final Logger logger = Logger.getLogger(RacesAssetsLoader.class.getSimpleName());

  private RacesAssetsLoader() {}

  private static AssetRegistry.BuildingVisuals registerBuildingVisuals(
      RenderQueues queues,
      String built_name,
      String halfbuilt_name,
      String start_name,
      float shadow_diameter,
      float no_detail_size) {
    final float ring_mid = 0.445f;
    final float fadeout = 0.002f;
    final float ring_thickness = RaceData.BUILDING_RING_PHYSICAL_THICKNESS / shadow_diameter;
    Supplier<Texture[]> building_shadow_desc =
        new GeneratorHalos(
            DecalRenderer.HALO_LUT_RESOLUTION,
            new float[][] {{0.15f, 0.5f}, {0.5f, 0f}},
            new float[][] {
              {ring_mid - ring_thickness / 2 - fadeout, 0f},
              {ring_mid - ring_thickness / 2, 1f},
              {ring_mid + ring_thickness / 2, 1f},
              {ring_mid + ring_thickness / 2 + fadeout, 0f}
            });
    ShadowListKey<SelectableShadowRenderer> shadow_renderer =
        queues.registerShadowRenderer(
            building_shadow_desc,
            SelectableShadowRenderer.class,
            () -> new SelectableShadowRenderer(building_shadow_desc));
    SpriteFile building = new SpriteFile(built_name, RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    SpriteFile building_halfbuilt =
        new SpriteFile(halfbuilt_name, RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    SpriteFile building_start = new SpriteFile(start_name, RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    SpriteKey builtSprite = queues.register(building);
    SpriteKey halfbuiltSprite = queues.register(building_halfbuilt);
    SpriteKey startSprite = queues.register(building_start);

    return new AssetRegistry.BuildingVisuals(
        startSprite, halfbuiltSprite, builtSprite, shadow_renderer, no_detail_size);
  }

  public static AssetRegistry.GameAssets load(RenderQueues queues, Map<EmojiType, SpriteKey> extraEmojis) {
    int num_progress = 23;
    SpriteFile native_rock_sprite =
        new SpriteFile(
            "/geometry/natives/rock_resource.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);
    SpriteFile native_wood_sprite =
        new SpriteFile(
            "/geometry/natives/wood_resource.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    SpriteFile native_rubber_sprite =
        new SpriteFile(
            "/geometry/natives/rubber_resource.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);

    EnumMap<SupplyType, SpriteKey> nativeCarriedSupplies = new EnumMap<>(SupplyType.class);
    nativeCarriedSupplies.put(SupplyType.WOOD, queues.register(native_wood_sprite));
    nativeCarriedSupplies.put(SupplyType.ROCK, queues.register(native_rock_sprite));
    nativeCarriedSupplies.put(SupplyType.IRON, queues.register(native_rock_sprite, 1));
    nativeCarriedSupplies.put(SupplyType.RUBBER, queues.register(native_rubber_sprite));

    SpriteFile viking_wood_sprite =
        new SpriteFile(
            "/geometry/vikings/wood_resource.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    SpriteFile viking_rubber_sprite =
        new SpriteFile(
            "/geometry/vikings/rubber_resource.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);
    SpriteFile viking_rock_sprite =
        new SpriteFile(
            "/geometry/vikings/rock_resource.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);

    EnumMap<SupplyType, SpriteKey> vikingCarriedSupplies = new EnumMap<>(SupplyType.class);
    vikingCarriedSupplies.put(SupplyType.WOOD, queues.register(viking_wood_sprite));
    vikingCarriedSupplies.put(SupplyType.ROCK, queues.register(viking_rock_sprite));
    vikingCarriedSupplies.put(SupplyType.IRON, queues.register(viking_rock_sprite, 1));
    vikingCarriedSupplies.put(SupplyType.RUBBER, queues.register(viking_rubber_sprite));

    List<TextureKey> smokeTextures = List.of(queues.registerEffectTexture(new GeneratorSmoke(42, 0.6f, 1.0f), 0, 0));

    List<TextureKey> damageSmokeTextures =
        List.of(queues.registerEffectTexture(new GeneratorSmoke(43, 1.0f, 0.5f), 0, 1));

    List<TextureKey> poisonTextures = List.of(queues.registerEffectTexture(new GeneratorPoison(), 0, 2));

    TextureKey lightningTexture = queues.registerTexture(new GeneratorLightning(), 0);

    List<TextureKey> noteTextures = new ArrayList<>(8);
    for (int i = 0; i < 8; i++) {
      noteTextures.add(queues.registerEffectTexture(TextureFile.forEffect("/textures/effects/note" + (i + 1)), 3 + i));
    }

    List<TextureKey> starTextures =
        List.of(queues.registerEffectTexture(TextureFile.forEffect("/textures/effects/star"), 11));

    queues.ensureTextureArray();
    ProgressListener.progress(1f / num_progress);

    EnumMap<BuildingType, AssetRegistry.BuildingVisuals> vikingBuildings = new EnumMap<>(BuildingType.class);
    vikingBuildings.put(
        BuildingType.QUARTERS,
        registerBuildingVisuals(
            queues,
            "/geometry/vikings/quarters.binsprite",
            "/geometry/vikings/quarters_halfbuilt.binsprite",
            "/geometry/vikings/quarters_start.binsprite",
            22f,
            6f));
    ProgressListener.progress(1f / num_progress);

    vikingBuildings.put(
        BuildingType.ARMORY,
        registerBuildingVisuals(
            queues,
            "/geometry/vikings/armory.binsprite",
            "/geometry/vikings/armory_halfbuilt.binsprite",
            "/geometry/vikings/armory_start.binsprite",
            22f,
            6f));
    ProgressListener.progress(1f / num_progress);

    vikingBuildings.put(
        BuildingType.TOWER,
        registerBuildingVisuals(
            queues,
            "/geometry/vikings/tower.binsprite",
            "/geometry/vikings/tower_halfbuilt.binsprite",
            "/geometry/vikings/tower_start.binsprite",
            10f,
            2.5f));
    ProgressListener.progress(1f / num_progress);

    EnumMap<BuildingType, AssetRegistry.BuildingVisuals> nativeBuildings = new EnumMap<>(BuildingType.class);
    nativeBuildings.put(
        BuildingType.QUARTERS,
        registerBuildingVisuals(
            queues,
            "/geometry/natives/quarters.binsprite",
            "/geometry/natives/quarters_halfbuilt.binsprite",
            "/geometry/natives/quarters_start.binsprite",
            16f,
            6f));
    ProgressListener.progress(1f / num_progress);

    nativeBuildings.put(
        BuildingType.ARMORY,
        registerBuildingVisuals(
            queues,
            "/geometry/natives/armory.binsprite",
            "/geometry/natives/armory_halfbuilt.binsprite",
            "/geometry/natives/armory_start.binsprite",
            16f,
            6f));
    ProgressListener.progress(1f / num_progress);

    nativeBuildings.put(
        BuildingType.TOWER,
        registerBuildingVisuals(
            queues,
            "/geometry/natives/tower.binsprite",
            "/geometry/natives/tower_halfbuilt.binsprite",
            "/geometry/natives/tower_start.binsprite",
            5f,
            2.5f));
    ProgressListener.progress(1f / num_progress);
    ProgressListener.progress(1f / num_progress);

    SpriteFile sprite_list_warrior =
        new SpriteFile("/geometry/vikings/warrior.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);

    SpriteFile sprite_list_chieftain =
        new SpriteFile("/geometry/vikings/chieftain.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    SpriteFile sprite_list_peon =
        new SpriteFile("/geometry/vikings/peon.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);

    SpriteFile sprite_list_native_peon =
        new SpriteFile("/geometry/natives/peon.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);

    SpriteFile sprite_list_native_warrior =
        new SpriteFile("/geometry/natives/warrior.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);

    SpriteFile sprite_list_native_chieftain =
        new SpriteFile("/geometry/natives/chieftain.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);

    SpriteFile viking_warrior_axe =
        new SpriteFile("/geometry/vikings/axe.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);

    SpriteFile native_warrior_spear =
        new SpriteFile("/geometry/natives/spear.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
    ProgressListener.progress(1f / num_progress);

    EnumMap<WeaponTier, SpriteKey> vikingWeapons = new EnumMap<>(WeaponTier.class);
    vikingWeapons.put(WeaponTier.ROCK, queues.register(viking_warrior_axe, UnitType.WARRIOR_ROCK.getValue()));
    vikingWeapons.put(WeaponTier.IRON, queues.register(viking_warrior_axe, UnitType.WARRIOR_IRON.getValue()));
    vikingWeapons.put(WeaponTier.RUBBER, queues.register(viking_warrior_axe, UnitType.WARRIOR_RUBBER.getValue()));

    EnumMap<WeaponTier, SpriteKey> nativeWeapons = new EnumMap<>(WeaponTier.class);
    nativeWeapons.put(WeaponTier.ROCK, queues.register(native_warrior_spear, UnitType.WARRIOR_ROCK.getValue()));
    nativeWeapons.put(WeaponTier.IRON, queues.register(native_warrior_spear, UnitType.WARRIOR_IRON.getValue()));
    nativeWeapons.put(WeaponTier.RUBBER, queues.register(native_warrior_spear, UnitType.WARRIOR_RUBBER.getValue()));

    ProgressListener.progress(1f / num_progress);

    EnumMap<UnitType, SpriteKey> vikingUnits = new EnumMap<>(UnitType.class);
    vikingUnits.put(UnitType.WARRIOR_ROCK, queues.register(sprite_list_warrior, UnitType.WARRIOR_ROCK.getValue()));
    vikingUnits.put(UnitType.WARRIOR_IRON, queues.register(sprite_list_warrior, UnitType.WARRIOR_IRON.getValue()));
    vikingUnits.put(UnitType.WARRIOR_RUBBER, queues.register(sprite_list_warrior, UnitType.WARRIOR_RUBBER.getValue()));
    vikingUnits.put(UnitType.PEON, queues.register(sprite_list_peon));
    vikingUnits.put(UnitType.CHIEFTAIN, queues.register(sprite_list_chieftain));

    EnumMap<UnitType, SpriteKey> nativeUnits = new EnumMap<>(UnitType.class);
    nativeUnits.put(
        UnitType.WARRIOR_ROCK, queues.register(sprite_list_native_warrior, UnitType.WARRIOR_ROCK.getValue()));
    nativeUnits.put(
        UnitType.WARRIOR_IRON, queues.register(sprite_list_native_warrior, UnitType.WARRIOR_IRON.getValue()));
    nativeUnits.put(
        UnitType.WARRIOR_RUBBER, queues.register(sprite_list_native_warrior, UnitType.WARRIOR_RUBBER.getValue()));
    nativeUnits.put(UnitType.PEON, queues.register(sprite_list_native_peon));
    nativeUnits.put(UnitType.CHIEFTAIN, queues.register(sprite_list_native_chieftain));

    ProgressListener.progress(1f / num_progress);
    SpriteKey nativeRallyPoint =
        queues.register(
            new SpriteFile(
                "/geometry/natives/rally_point.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));

    SpriteKey vikingRallyPoint =
        queues.register(
            new SpriteFile(
                "/geometry/vikings/rally_point.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));

    List<SpriteKey> woodFragments =
        List.of(
            queues.register(
                new SpriteFile(
                    "/geometry/misc/wood_2.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false),
                0),
            queues.register(
                new SpriteFile(
                    "/geometry/misc/wood_3.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)),
            queues.register(
                new SpriteFile(
                    "/geometry/misc/wood_4.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)),
            queues.register(
                new SpriteFile(
                    "/geometry/misc/wood_5.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)));

    List<SpriteKey> treasures =
        List.of(
            queues.register(
                new SpriteFile(
                    "/geometry/misc/icon.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)),
            queues.register(
                new SpriteFile(
                    "/geometry/misc/treasure_1.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)),
            queues.register(
                new SpriteFile(
                    "/geometry/misc/treasure_2.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)),
            queues.register(
                new SpriteFile(
                    "/geometry/misc/treasure_3.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)),
            queues.register(
                new SpriteFile(
                    "/geometry/misc/treasure_4.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)),
            queues.register(
                new SpriteFile(
                    "/geometry/misc/treasure_5.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false)));

    logger.info("RaceData: beginning emoji sprite registration");
    SpriteKey gravestoneEmojiSprite =
        queues.registerDynamicSprite(
            SpriteList.getQuadInstance(), queues.registerTexture(new ColorGraphemeGenerator("🪦"), 0));
    List<SpriteKey> chickenEmojiSprites =
        "🐓🥚🐣🌽🐛"
            .codePoints()
            .mapToObj(ColorGraphemeGenerator::new)
            .map(tg -> queues.registerTexture(tg, 0))
            .map(tk -> queues.registerDynamicSprite(SpriteList.getQuadInstance(), tk))
            .toList();
    SpriteKey sawEmojiSprite =
        queues.registerDynamicSprite(
            SpriteList.getQuadInstance(), queues.registerTexture(new ColorGraphemeGenerator("🪚"), 0));
    SpriteKey hammerEmojiSprite =
        queues.registerDynamicSprite(
            SpriteList.getQuadInstance(), queues.registerTexture(new ColorGraphemeGenerator("🔨"), 0));

    EnumMap<EmojiType, SpriteKey> emojisMap = new EnumMap<>(EmojiType.class);
    emojisMap.putAll(extraEmojis);
    emojisMap.put(EmojiType.GRAVESTONE, gravestoneEmojiSprite);
    emojisMap.put(EmojiType.REPAIR_SAW, sawEmojiSprite);
    emojisMap.put(EmojiType.REPAIR_HAMMER, hammerEmojiSprite);

    EnumMap<Race, AssetRegistry.RaceVisuals> races = new EnumMap<>(Race.class);
    races.put(
        Race.VIKINGS,
        new AssetRegistry.RaceVisuals(
            vikingUnits, vikingBuildings, vikingWeapons, vikingCarriedSupplies, vikingRallyPoint));
    races.put(
        Race.NATIVES,
        new AssetRegistry.RaceVisuals(
            nativeUnits, nativeBuildings, nativeWeapons, nativeCarriedSupplies, nativeRallyPoint));

    var effects =
        new AssetRegistry.EffectAssets(
            smokeTextures,
            damageSmokeTextures,
            poisonTextures,
            lightningTexture,
            noteTextures,
            starTextures,
            woodFragments);
    var emojis = new AssetRegistry.EmojiAssets(emojisMap, chickenEmojiSprites);

    ProgressListener.progress(1f / num_progress);
    ProgressListener.progress(1f / num_progress);
    return new AssetRegistry.GameAssets(races, effects, treasures, emojis);
  }
}
