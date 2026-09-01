package com.oddlabs.tt.engine.resource;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.engine.procedural.GeneratorCrack;
import com.oddlabs.tt.engine.procedural.GeneratorHalos;
import com.oddlabs.tt.engine.render.*;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.EmojiType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.model.UnitVisualType;
import com.oddlabs.tt.simulation.model.WeaponVisualType;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Registry mapping simulation visual and audio asset types to graphics-bound SpriteKeys, ShadowListKeys,
 * textures, and race audio configurations.
 */
public final class AssetRegistry {
    public static final GeneratorHalos DEFAULT_SHADOW_DESC = new GeneratorHalos(DecalRenderer.HALO_LUT_RESOLUTION,
            new float[][]{{0f, 0.75f}, {0.5f, 0f}}, new float[][]{{0.40f, 0f}, {0.41f, 1f}, {0.48f, 1f}, {0.49f, 0f}});

    public static final GeneratorCrack CRACK_DECAL_DESC = new GeneratorCrack();

    private static final AssetRegistry INSTANCE = new AssetRegistry();

    public static AssetRegistry getInstance() {
        return INSTANCE;
    }

    public record BuildingVisuals(
                                  SpriteKey start,
                                  SpriteKey halfbuilt,
                                  SpriteKey built,
                                  ShadowListKey shadow
    ) {
    }

    /**
     * Presentation audio configurations for a playable race.
     */
    public record RaceAudio(
                            AudioParameters attackNotification,
                            AudioParameters buildingNotification,
                            AudioParameters music
    ) {
    }

    private final EnumMap<Race, EnumMap<UnitVisualType, SpriteKey>> units = new EnumMap<>(Race.class);
    private final EnumMap<Race, EnumMap<BuildingType, BuildingVisuals>> buildings = new EnumMap<>(Race.class);
    private final EnumMap<Race, EnumMap<WeaponVisualType, SpriteKey>> weapons = new EnumMap<>(Race.class);
    private final EnumMap<Race, EnumMap<SupplyType, SpriteKey>> carriedSupplies = new EnumMap<>(Race.class);
    private final EnumMap<EmojiType, SpriteKey> emojis = new EnumMap<>(EmojiType.class);
    private final EnumMap<Race, SpriteKey> rallyPoints = new EnumMap<>(Race.class);
    private final EnumMap<Race, RaceAudio> raceAudio = new EnumMap<>(Race.class);
    private final EnumMap<Terrain, SpriteKey[]> plantSprites = new EnumMap<>(Terrain.class);
    private SpriteKey @Nullable [] rockFragments;
    private SpriteKey @Nullable [] ironFragments;
    private @Nullable SpriteKey chickenSprite;
    private SpriteKey @Nullable [] chickenCluckSprites;
    private @Nullable ShadowListKey defaultUnitShadow;
    private TextureKey @Nullable [] smokeTextures;
    private TextureKey @Nullable [] damageSmokeTextures;
    private TextureKey @Nullable [] poisonTextures;
    private @Nullable TextureKey lightningTexture;
    private TextureKey @Nullable [] noteTextures;
    private TextureKey @Nullable [] starTextures;
    private SpriteKey @Nullable [] woodFragments;
    private SpriteKey @Nullable [] treasures;

    private AssetRegistry() {
        for (Race race : Race.values()) {
            units.put(race, new EnumMap<>(UnitVisualType.class));
            buildings.put(race, new EnumMap<>(BuildingType.class));
            weapons.put(race, new EnumMap<>(WeaponVisualType.class));
            carriedSupplies.put(race, new EnumMap<>(SupplyType.class));
        }
    }

    public void registerChickenCluckSprites(SpriteKey[] sprites) {
        this.chickenCluckSprites = sprites.clone();
    }

    public void registerWeapon(Race race, WeaponVisualType type, SpriteKey sprite) {
        weapons.get(race).put(type, sprite);
    }

    public SpriteKey getWeaponSprite(Race race, WeaponVisualType type) {
        SpriteKey sprite = weapons.get(race).get(type);
        if (sprite == null) {
            throw new IllegalStateException("Weapon sprite not registered for race " + race + " and type " + type);
        }
        return sprite;
    }

    public void registerRallyPoint(Race race, SpriteKey sprite) {
        rallyPoints.put(race, sprite);
    }

    public SpriteKey getRallyPoint(Race race) {
        SpriteKey sprite = rallyPoints.get(race);
        if (sprite == null) {
            throw new IllegalStateException("Rally point sprite not registered for race " + race);
        }
        return sprite;
    }

    public void registerUnit(Race race, UnitVisualType type, SpriteKey sprite) {
        units.get(race).put(type, sprite);
    }

    public SpriteKey getUnitSprite(Race race, UnitVisualType type) {
        SpriteKey sprite = units.get(race).get(type);
        if (sprite == null) {
            throw new IllegalStateException("Unit sprite not registered for race " + race + " and type " + type);
        }
        return sprite;
    }

    public void registerBuilding(Race race, BuildingType type,
            BuildingVisuals visuals) {
        buildings.get(race).put(type, visuals);
    }

    public BuildingVisuals getBuildingVisuals(Race race, BuildingType type) {
        BuildingVisuals visuals = buildings.get(race).get(type);
        if (visuals == null) {
            throw new IllegalStateException("Building visuals not registered for race " + race + " and type " + type);
        }
        return visuals;
    }

    public void registerEmoji(EmojiType type, SpriteKey sprite) {
        emojis.put(type, sprite);
    }

    public Optional<SpriteKey> getEmojiSprite(EmojiType type) {
        if (type == EmojiType.CHICKEN_CLUCK && chickenCluckSprites != null && chickenCluckSprites.length > 0) {
            return Optional.of(chickenCluckSprites[ThreadLocalRandom.current().nextInt(
                    chickenCluckSprites.length)]);
        }

        return Optional.ofNullable(emojis.get(type));
    }

    public void registerDefaultUnitShadow(ShadowListKey shadow) {
        this.defaultUnitShadow = shadow;
    }

    public ShadowListKey getDefaultUnitShadow() {
        if (defaultUnitShadow == null) {
            throw new IllegalStateException("Default unit shadow not registered");
        }
        return defaultUnitShadow;
    }

    public void registerCarriedSupply(Race race, SupplyType type, SpriteKey sprite) {
        carriedSupplies.get(race).put(type, sprite);
    }

    public SpriteKey getCarriedSupplySprite(Race race, SupplyType type) {
        SpriteKey sprite = carriedSupplies.get(race).get(type);
        if (sprite == null) {
            throw new IllegalStateException("Carried supply sprite not registered for race " + race + " and type "
                    + type);
        }
        return sprite;
    }

    public void registerSmokeTextures(TextureKey[] textures) {
        this.smokeTextures = textures.clone();
    }

    public TextureKey[] getSmokeTextures() {
        if (smokeTextures == null) {
            throw new IllegalStateException("Smoke textures not registered");
        }
        return smokeTextures;
    }

    public void registerDamageSmokeTextures(TextureKey[] textures) {
        this.damageSmokeTextures = textures.clone();
    }

    public TextureKey[] getDamageSmokeTextures() {
        if (damageSmokeTextures == null) {
            throw new IllegalStateException("Damage smoke textures not registered");
        }
        return damageSmokeTextures;
    }

    public void registerPoisonTextures(TextureKey[] textures) {
        this.poisonTextures = textures.clone();
    }

    public TextureKey[] getPoisonTextures() {
        if (poisonTextures == null) {
            throw new IllegalStateException("Poison textures not registered");
        }
        return poisonTextures;
    }

    public void registerLightningTexture(TextureKey texture) {
        this.lightningTexture = texture;
    }

    public TextureKey getLightningTexture() {
        if (lightningTexture == null) {
            throw new IllegalStateException("Lightning texture not registered");
        }
        return lightningTexture;
    }

    public void registerNoteTextures(TextureKey[] textures) {
        this.noteTextures = textures.clone();
    }

    public TextureKey[] getNoteTextures() {
        if (noteTextures == null) {
            throw new IllegalStateException("Note textures not registered");
        }
        return noteTextures;
    }

    public void registerStarTextures(TextureKey[] textures) {
        this.starTextures = textures.clone();
    }

    public TextureKey[] getStarTextures() {
        if (starTextures == null) {
            throw new IllegalStateException("Star textures not registered");
        }
        return starTextures;
    }

    public void registerWoodFragments(SpriteKey[] sprites) {
        this.woodFragments = sprites.clone();
    }

    public SpriteKey[] getWoodFragments() {
        if (woodFragments == null) {
            throw new IllegalStateException("Wood fragments not registered");
        }
        return woodFragments;
    }

    public void registerTreasures(SpriteKey[] sprites) {
        this.treasures = sprites.clone();
    }

    public SpriteKey[] getTreasures() {
        if (treasures == null) {
            throw new IllegalStateException("Treasures not registered");
        }
        return treasures;
    }

    public void registerRaceAudio(Race race, AudioParameters attackNotification,
            AudioParameters buildingNotification, AudioParameters music) {
        raceAudio.put(race, new RaceAudio(attackNotification, buildingNotification, music));
    }

    public RaceAudio getRaceAudio(Race race) {
        RaceAudio audio = raceAudio.get(race);
        if (audio == null) {
            throw new IllegalStateException("Race audio not registered for race " + race);
        }
        return audio;
    }

    public AudioParameters getAttackNotificationAudio(Race race) {
        return getRaceAudio(race).attackNotification();
    }

    public AudioParameters getBuildingNotificationAudio(Race race) {
        return getRaceAudio(race).buildingNotification();
    }

    public AudioParameters getMusic(Race race) {
        return getRaceAudio(race).music();
    }

    public void registerRockFragments(SpriteKey[] sprites) {
        this.rockFragments = sprites.clone();
    }

    public SpriteKey getRockFragmentSprite(int index) {
        if (rockFragments == null) {
            throw new IllegalStateException("Rock fragments not registered");
        }
        return rockFragments[index % rockFragments.length];
    }

    public void registerIronFragments(SpriteKey[] sprites) {
        this.ironFragments = sprites.clone();
    }

    public SpriteKey getIronFragmentSprite(int index) {
        if (ironFragments == null) {
            throw new IllegalStateException("Iron fragments not registered");
        }
        return ironFragments[index % ironFragments.length];
    }

    public void registerPlants(Terrain terrain, SpriteKey[] sprites) {
        plantSprites.put(terrain, sprites.clone());
    }

    public SpriteKey getPlantSprite(Terrain terrain, int index) {
        SpriteKey[] sprites = plantSprites.get(terrain);
        if (sprites == null) {
            throw new IllegalStateException("Plant sprites not registered for terrain " + terrain);
        }
        return sprites[index % sprites.length];
    }

    public void registerChicken(SpriteKey sprite) {
        this.chickenSprite = sprite;
    }

    public SpriteKey getChickenSprite() {
        if (chickenSprite == null) {
            throw new IllegalStateException("Chicken sprite not registered");
        }
        return chickenSprite;
    }
}
