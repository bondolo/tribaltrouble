package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.client.gui.GUIIcons;
import com.oddlabs.tt.engine.font.ColorGraphemeGenerator;
import com.oddlabs.tt.engine.procedural.GeneratorHalos;
import com.oddlabs.tt.engine.procedural.GeneratorLightning;
import com.oddlabs.tt.engine.procedural.GeneratorPoison;
import com.oddlabs.tt.engine.procedural.GeneratorSmoke;
import com.oddlabs.tt.engine.render.DecalRenderer;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.ShadowListKey;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.engine.resource.TextureFile;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.EmojiType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.UnitVisualType;
import com.oddlabs.tt.simulation.model.WeaponVisualType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.EnumMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Loader class that centralizes client-side graphics and audio asset registration.
 */
public final class RacesAssetsLoader {
    private static final Logger logger = Logger.getLogger(RacesAssetsLoader.class.getSimpleName());

    private RacesAssetsLoader() {
    }

    public static String getRaceName(Race race) {
        return RaceData.getRaceName(race);
    }

    private static void registerBuildingVisuals(
            RenderQueues queues,
            Race race,
            BuildingType building_type,
            String built_name,
            String halfbuilt_name,
            String start_name,
            float shadow_diameter
    ) {
        final float ring_mid = 0.445f;
        final float fadeout = 0.002f;
        final float ring_thickness = RaceData.BUILDING_RING_PHYSICAL_THICKNESS / shadow_diameter;
        Supplier<Texture[]> building_shadow_desc = new GeneratorHalos(
                DecalRenderer.HALO_LUT_RESOLUTION,
                new float[][]{{0.15f, 0.5f}, {0.5f, 0f}},
                new float[][]{
                        {ring_mid - ring_thickness / 2 - fadeout, 0f},
                        {ring_mid - ring_thickness / 2, 1f},
                        {ring_mid + ring_thickness / 2, 1f},
                        {ring_mid + ring_thickness / 2 + fadeout, 0f}
                }
        );
        ShadowListKey shadow_renderer = queues.registerShadowRenderer(
                building_shadow_desc,
                new SelectableShadowRenderer(building_shadow_desc)
        );
        SpriteFile building = new SpriteFile(built_name, RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        SpriteFile building_halfbuilt = new SpriteFile(halfbuilt_name, RenderConfig.NO_MIPMAP_CUTOFF, true, true, true,
                false);
        SpriteFile building_start = new SpriteFile(start_name, RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        SpriteKey builtSprite = queues.register(building);
        SpriteKey halfbuiltSprite = queues.register(building_halfbuilt);
        SpriteKey startSprite = queues.register(building_start);

        AssetRegistry.getInstance().registerBuilding(
                race,
                building_type,
                new AssetRegistry.BuildingVisuals(startSprite, halfbuiltSprite, builtSprite, shadow_renderer)
        );
    }

    public static RaceData load(RenderQueues queues) {
        int num_progress = 23;
        SpriteFile native_rock_sprite = new SpriteFile("/geometry/natives/rock_resource.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);
        SpriteFile native_wood_sprite = new SpriteFile("/geometry/natives/wood_resource.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        SpriteFile native_rubber_sprite = new SpriteFile("/geometry/natives/rubber_resource.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);

        EnumMap<SupplyType, SpriteKey> nativeMap = new EnumMap<>(SupplyType.class);
        nativeMap.put(SupplyType.WOOD, queues.register(native_wood_sprite));
        nativeMap.put(SupplyType.ROCK, queues.register(native_rock_sprite));
        nativeMap.put(SupplyType.IRON, queues.register(native_rock_sprite, 1));
        nativeMap.put(SupplyType.RUBBER, queues.register(native_rubber_sprite));
        for (var entry : nativeMap.entrySet()) {
            AssetRegistry.getInstance().registerCarriedSupply(Race.NATIVES, entry.getKey(), entry.getValue());
        }

        SpriteFile viking_wood_sprite = new SpriteFile("/geometry/vikings/wood_resource.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        SpriteFile viking_rubber_sprite = new SpriteFile("/geometry/vikings/rubber_resource.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);
        SpriteFile viking_rock_sprite = new SpriteFile("/geometry/vikings/rock_resource.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);

        EnumMap<SupplyType, SpriteKey> vikingMap = new EnumMap<>(SupplyType.class);
        vikingMap.put(SupplyType.WOOD, queues.register(viking_wood_sprite));
        vikingMap.put(SupplyType.ROCK, queues.register(viking_rock_sprite));
        vikingMap.put(SupplyType.IRON, queues.register(viking_rock_sprite, 1));
        vikingMap.put(SupplyType.RUBBER, queues.register(viking_rubber_sprite));
        for (var entry : vikingMap.entrySet()) {
            AssetRegistry.getInstance().registerCarriedSupply(Race.VIKINGS, entry.getKey(), entry.getValue());
        }

        TextureKey[] smoke_textures = new TextureKey[1];
        smoke_textures[0] = queues.registerEffectTexture(new GeneratorSmoke(42, 0.6f, 1.0f), 0, 0);
        AssetRegistry.getInstance().registerSmokeTextures(smoke_textures);

        TextureKey[] damage_smoke_textures = new TextureKey[1];
        damage_smoke_textures[0] = queues.registerEffectTexture(new GeneratorSmoke(43, 1.0f, 0.5f), 0, 1);
        AssetRegistry.getInstance().registerDamageSmokeTextures(damage_smoke_textures);

        TextureKey[] poison_textures = new TextureKey[1];
        poison_textures[0] = queues.registerEffectTexture(new GeneratorPoison(), 0, 2);
        AssetRegistry.getInstance().registerPoisonTextures(poison_textures);

        TextureKey lightning_texture = queues.registerEffectTexture(new GeneratorLightning(), 0, 3);
        AssetRegistry.getInstance().registerLightningTexture(lightning_texture);

        TextureKey[] note_textures = new TextureKey[8];
        for (int i = 0; i < note_textures.length; i++) {
            note_textures[i] = queues.registerEffectTexture(
                    new TextureFile("/textures/effects/note" + (i + 1),
                            RenderConfig.COMPRESSED_RGBA_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR,
                            GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), 4 + i);
        }
        AssetRegistry.getInstance().registerNoteTextures(note_textures);

        TextureKey[] star_textures = new TextureKey[1];
        star_textures[0] = queues.registerEffectTexture(
                new TextureFile("/textures/effects/star",
                        RenderConfig.COMPRESSED_RGBA_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR,
                        GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), 12);
        AssetRegistry.getInstance().registerStarTextures(star_textures);

        queues.ensureTextureArray();
        ProgressListener.progress(1f / num_progress);

        registerBuildingVisuals(
                queues, Race.VIKINGS, BuildingType.QUARTERS,
                "/geometry/vikings/quarters.binsprite",
                "/geometry/vikings/quarters_halfbuilt.binsprite",
                "/geometry/vikings/quarters_start.binsprite",
                22f);
        ProgressListener.progress(1f / num_progress);

        registerBuildingVisuals(
                queues, Race.VIKINGS, BuildingType.ARMORY,
                "/geometry/vikings/armory.binsprite",
                "/geometry/vikings/armory_halfbuilt.binsprite",
                "/geometry/vikings/armory_start.binsprite",
                22f);
        ProgressListener.progress(1f / num_progress);

        registerBuildingVisuals(
                queues, Race.VIKINGS, BuildingType.TOWER,
                "/geometry/vikings/tower.binsprite",
                "/geometry/vikings/tower_halfbuilt.binsprite",
                "/geometry/vikings/tower_start.binsprite", 10f);
        ProgressListener.progress(1f / num_progress);

        registerBuildingVisuals(
                queues, Race.NATIVES, BuildingType.QUARTERS,
                "/geometry/natives/quarters.binsprite",
                "/geometry/natives/quarters_halfbuilt.binsprite",
                "/geometry/natives/quarters_start.binsprite",
                16f);
        ProgressListener.progress(1f / num_progress);

        registerBuildingVisuals(
                queues, Race.NATIVES, BuildingType.ARMORY,
                "/geometry/natives/armory.binsprite",
                "/geometry/natives/armory_halfbuilt.binsprite",
                "/geometry/natives/armory_start.binsprite",
                16f);
        ProgressListener.progress(1f / num_progress);

        registerBuildingVisuals(
                queues, Race.NATIVES, BuildingType.TOWER,
                "/geometry/natives/tower.binsprite",
                "/geometry/natives/tower_halfbuilt.binsprite",
                "/geometry/natives/tower_start.binsprite",
                5f);
        ProgressListener.progress(1f / num_progress);
        ProgressListener.progress(1f / num_progress);

        SpriteFile sprite_list_warrior = new SpriteFile("/geometry/vikings/warrior.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);

        SpriteFile sprite_list_chieftain = new SpriteFile("/geometry/vikings/chieftain.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        SpriteFile sprite_list_peon = new SpriteFile("/geometry/vikings/peon.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);

        SpriteFile sprite_list_native_peon = new SpriteFile("/geometry/natives/peon.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);

        SpriteFile sprite_list_native_warrior = new SpriteFile("/geometry/natives/warrior.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);

        SpriteFile sprite_list_native_chieftain = new SpriteFile("/geometry/natives/chieftain.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);

        SpriteFile viking_warrior_axe = new SpriteFile("/geometry/vikings/axe.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);

        SpriteFile native_warrior_spear = new SpriteFile("/geometry/natives/spear.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false);
        ProgressListener.progress(1f / num_progress);

        AssetRegistry.getInstance().registerWeapon(
                Race.VIKINGS, WeaponVisualType.ROCK,
                queues.register(viking_warrior_axe, UnitType.WARRIOR_ROCK.getValue()));
        AssetRegistry.getInstance().registerWeapon(
                Race.VIKINGS, WeaponVisualType.IRON,
                queues.register(viking_warrior_axe, UnitType.WARRIOR_IRON.getValue()));
        AssetRegistry.getInstance().registerWeapon(
                Race.VIKINGS, WeaponVisualType.RUBBER,
                queues.register(viking_warrior_axe, UnitType.WARRIOR_RUBBER.getValue()));

        AssetRegistry.getInstance().registerWeapon(
                Race.NATIVES, WeaponVisualType.ROCK,
                queues.register(native_warrior_spear, UnitType.WARRIOR_ROCK.getValue()));
        AssetRegistry.getInstance().registerWeapon(
                Race.NATIVES, WeaponVisualType.IRON,
                queues.register(native_warrior_spear, UnitType.WARRIOR_IRON.getValue()));
        AssetRegistry.getInstance().registerWeapon(
                Race.NATIVES, WeaponVisualType.RUBBER,
                queues.register(native_warrior_spear, UnitType.WARRIOR_RUBBER.getValue())
        );

        ProgressListener.progress(1f / num_progress);
        ShadowListKey default_shadow_list = queues.registerShadowRenderer(
                AssetRegistry.DEFAULT_SHADOW_DESC,
                new SelectableShadowRenderer(AssetRegistry.DEFAULT_SHADOW_DESC)
        );
        AssetRegistry.getInstance().registerDefaultUnitShadow(default_shadow_list);

        SpriteKey vRockSprite = queues.register(sprite_list_warrior, UnitType.WARRIOR_ROCK.getValue());
        AssetRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.WARRIOR_ROCK, vRockSprite);

        SpriteKey vIronSprite = queues.register(sprite_list_warrior, UnitType.WARRIOR_IRON.getValue());
        AssetRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.WARRIOR_IRON, vIronSprite);

        SpriteKey vRubberSprite = queues.register(sprite_list_warrior, UnitType.WARRIOR_RUBBER.getValue());
        AssetRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.WARRIOR_RUBBER, vRubberSprite);

        SpriteKey nRockSprite = queues.register(sprite_list_native_warrior, UnitType.WARRIOR_ROCK.getValue());
        AssetRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.WARRIOR_ROCK, nRockSprite);

        SpriteKey nIronSprite = queues.register(sprite_list_native_warrior, UnitType.WARRIOR_IRON.getValue());
        AssetRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.WARRIOR_IRON, nIronSprite);

        SpriteKey nRubberSprite = queues.register(sprite_list_native_warrior, UnitType.WARRIOR_RUBBER.getValue());
        AssetRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.WARRIOR_RUBBER, nRubberSprite);

        SpriteKey vPeonSprite = queues.register(sprite_list_peon);
        AssetRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.PEON, vPeonSprite);

        SpriteKey nPeonSprite = queues.register(sprite_list_native_peon);
        AssetRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.PEON, nPeonSprite);

        SpriteKey vChieftainSprite = queues.register(sprite_list_chieftain);
        AssetRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.CHIEFTAIN, vChieftainSprite);

        SpriteKey nChieftainSprite = queues.register(sprite_list_native_chieftain);
        AssetRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.CHIEFTAIN, nChieftainSprite);

        ProgressListener.progress(1f / num_progress);
        GUIIcons icons = GUIIcons.getIcons();
        SpriteKey native_rally_point = queues.register(
                new SpriteFile("/geometry/natives/rally_point.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true,
                        true, false));
        AssetRegistry.getInstance().registerRallyPoint(Race.NATIVES, native_rally_point);

        AssetRegistry.getInstance().registerRaceAudio(
                Race.NATIVES,
                new AudioParameters(AudioAssets.SFX_ATTACKNOTIFY_NATIVE, AudioAssets.AUDIO_RANK_NOTIFICATION,
                        AudioAssets.AUDIO_DISTANCE_NOTIFICATION, AudioAssets.AUDIO_GAIN_NOTIFICATION,
                        AudioAssets.AUDIO_RADIUS_NOTIFICATION,
                        1f, false, true),
                new AudioParameters(AudioAssets.SFX_BUILDINGNOTIFY_NATIVE, AudioAssets.AUDIO_RANK_NOTIFICATION,
                        AudioAssets.AUDIO_DISTANCE_NOTIFICATION, AudioAssets.AUDIO_GAIN_NOTIFICATION,
                        AudioAssets.AUDIO_RADIUS_NOTIFICATION,
                        1f, false, true),
                AudioAssets.MUSIC_NATIVE
        );

        SpriteKey viking_rally_point = queues.register(
                new SpriteFile("/geometry/vikings/rally_point.binsprite", RenderConfig.NO_MIPMAP_CUTOFF, true, true,
                        true, false)
        );
        AssetRegistry.getInstance().registerRallyPoint(Race.VIKINGS, viking_rally_point);

        AssetRegistry.getInstance().registerRaceAudio(
                Race.VIKINGS,
                new AudioParameters(AudioAssets.SFX_ATTACKNOTIFY_VIKING, AudioAssets.AUDIO_RANK_NOTIFICATION,
                        AudioAssets.AUDIO_DISTANCE_NOTIFICATION, AudioAssets.AUDIO_GAIN_NOTIFICATION,
                        AudioAssets.AUDIO_RADIUS_NOTIFICATION,
                        1f, false, true),
                new AudioParameters(AudioAssets.SFX_BUILDINGNOTIFY_VIKING, AudioAssets.AUDIO_RANK_NOTIFICATION,
                        AudioAssets.AUDIO_DISTANCE_NOTIFICATION, AudioAssets.AUDIO_GAIN_NOTIFICATION,
                        AudioAssets.AUDIO_RADIUS_NOTIFICATION,
                        1f, false, true),
                AudioAssets.MUSIC_VIKING
        );

        SpriteKey[] wood_fragment_sprites = new SpriteKey[4];
        wood_fragment_sprites[0] = queues.register(new SpriteFile("/geometry/misc/wood_2.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false), 0);
        wood_fragment_sprites[1] = queues.register(new SpriteFile("/geometry/misc/wood_3.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        wood_fragment_sprites[2] = queues.register(new SpriteFile("/geometry/misc/wood_4.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        wood_fragment_sprites[3] = queues.register(new SpriteFile("/geometry/misc/wood_5.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        AssetRegistry.getInstance().registerWoodFragments(wood_fragment_sprites);

        SpriteKey[] treasure_sprites = new SpriteKey[6];
        treasure_sprites[0] = queues.register(new SpriteFile("/geometry/misc/icon.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        treasure_sprites[1] = queues.register(new SpriteFile("/geometry/misc/treasure_1.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        treasure_sprites[2] = queues.register(new SpriteFile("/geometry/misc/treasure_2.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        treasure_sprites[3] = queues.register(new SpriteFile("/geometry/misc/treasure_3.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        treasure_sprites[4] = queues.register(new SpriteFile("/geometry/misc/treasure_4.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        treasure_sprites[5] = queues.register(new SpriteFile("/geometry/misc/treasure_5.binsprite",
                RenderConfig.NO_MIPMAP_CUTOFF, true, true, true, false));
        AssetRegistry.getInstance().registerTreasures(treasure_sprites);

        logger.info("RaceData: beginning emoji sprite registration");
        SpriteKey gravestone_emoji_sprite = queues.registerDynamicSprite(
                SpriteList.getQuadInstance(),
                queues.registerTexture(new ColorGraphemeGenerator("🪦"), 0)
        );
        SpriteKey[] chicken_emoji_sprites = "🐓🥚🐣🌽🐛".codePoints()
                .mapToObj(ColorGraphemeGenerator::new)
                .map(tg -> queues.registerTexture(tg, 0))
                .map(tk -> queues.registerDynamicSprite(SpriteList.getQuadInstance(), tk))
                .toArray(SpriteKey[]::new);
        SpriteKey saw_emoji_sprite = queues.registerDynamicSprite(
                SpriteList.getQuadInstance(),
                queues.registerTexture(new ColorGraphemeGenerator("🪚"), 0)
        );
        SpriteKey hammer_emoji_sprite = queues.registerDynamicSprite(
                SpriteList.getQuadInstance(),
                queues.registerTexture(new ColorGraphemeGenerator("🔨"), 0)
        );

        SpriteKey tree_status_sprite = registerIconSprite(queues, icons.getTreeStatusIcon());
        SpriteKey rock_status_sprite = registerIconSprite(queues, icons.getRockStatusIcon());
        SpriteKey iron_status_sprite = registerIconSprite(queues, icons.getIronStatusIcon());
        SpriteKey rubber_status_sprite = registerIconSprite(queues, icons.getRubberStatusIcon());

        AssetRegistry ar = AssetRegistry.getInstance();
        ar.registerEmoji(EmojiType.GRAVESTONE, gravestone_emoji_sprite);
        ar.registerEmoji(EmojiType.REPAIR_SAW, saw_emoji_sprite);
        ar.registerEmoji(EmojiType.REPAIR_HAMMER, hammer_emoji_sprite);
        ar.registerChickenCluckSprites(chicken_emoji_sprites);
        ar.registerEmoji(EmojiType.HARVEST_WOOD, tree_status_sprite);
        ar.registerEmoji(EmojiType.HARVEST_ROCK, rock_status_sprite);
        ar.registerEmoji(EmojiType.HARVEST_IRON, iron_status_sprite);
        ar.registerEmoji(EmojiType.HARVEST_RUBBER, rubber_status_sprite);

        ProgressListener.progress(1f / num_progress);
        ProgressListener.progress(1f / num_progress);

        return new RaceData();
    }

    private static SpriteKey registerIconSprite(RenderQueues queues, IconQuad icon) {
        return queues.registerQuadSprite(icon.getU1(), icon.getV1(), icon.getU2(), icon.getV2(), icon.getTexture());
    }
}
