package com.oddlabs.tt.client.resource;

import com.oddlabs.tt.client.render.SelectableShadowRenderer;
import com.oddlabs.tt.engine.render.ShadowListKey;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.CaptiveScenery;
import com.oddlabs.tt.simulation.model.GodScenery;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Plants;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RallyPointScenery;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.SceneryModel;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.StatueScenery;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.WeaponTier;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.jspecify.annotations.Nullable;

/// Immutable, session-scoped registry mapping simulation entity types to graphics-bound {@link SpriteKey}, {@link
/// ShadowListKey}, and {@link TextureKey} handles.
public final class AssetRegistry {
  /// Visual asset keys and no-detail size for a building type across construction stages.
  public record BuildingVisuals(
      SpriteKey start,
      SpriteKey halfbuilt,
      SpriteKey built,
      ShadowListKey<SelectableShadowRenderer> shadow,
      float noDetailSize) {
    public BuildingVisuals {
      Objects.requireNonNull(start, "start");
      Objects.requireNonNull(halfbuilt, "halfbuilt");
      Objects.requireNonNull(built, "built");
      Objects.requireNonNull(shadow, "shadow");
    }
  }

  /// Landscape-level visual assets shared across the main menu and full game worlds.
  public record LandscapeAssets(
      List<SpriteKey> rockFragments,
      List<SpriteKey> ironFragments,
      Map<Terrain, List<SpriteKey>> plantSprites,
      SpriteKey chickenSprite,
      ShadowListKey<SelectableShadowRenderer> defaultShadow) {
    public LandscapeAssets {
      rockFragments = copyNonEmpty(rockFragments, "rockFragments");
      ironFragments = copyNonEmpty(ironFragments, "ironFragments");
      Objects.requireNonNull(plantSprites, "plantSprites");
      EnumMap<Terrain, List<SpriteKey>> plantsCopy = new EnumMap<>(Terrain.class);
      for (Terrain terrain : Terrain.values()) {
        List<SpriteKey> sprites = plantSprites.get(terrain);
        if (sprites == null) {
          throw new IllegalArgumentException("plantSprites missing entry for " + terrain);
        }
        plantsCopy.put(terrain, copyNonEmpty(sprites, "plantSprites[" + terrain + "]"));
      }
      plantSprites = Collections.unmodifiableMap(plantsCopy);
      Objects.requireNonNull(chickenSprite, "chickenSprite");
      Objects.requireNonNull(defaultShadow, "defaultShadow");
    }
  }

  /// Race-specific visual assets for units, buildings, weapons, carried supplies, and rally points.
  public record RaceVisuals(
      Map<UnitType, SpriteKey> units,
      Map<BuildingType, BuildingVisuals> buildings,
      Map<WeaponTier, SpriteKey> weapons,
      Map<SupplyType, SpriteKey> carriedSupplies,
      SpriteKey rallyPoint) {
    public RaceVisuals {
      units = copyCompleteEnumMap(UnitType.class, units, "units");
      buildings = copyCompleteEnumMap(BuildingType.class, buildings, "buildings");
      weapons = copyCompleteEnumMap(WeaponTier.class, weapons, "weapons");
      carriedSupplies = copyCompleteEnumMap(SupplyType.class, carriedSupplies, "carriedSupplies");
      Objects.requireNonNull(rallyPoint, "rallyPoint");
    }
  }

  /// Particle and effect texture/sprite keys used during in-game simulation rendering.
  public record EffectAssets(
      List<TextureKey> smokeTextures,
      List<TextureKey> damageSmokeTextures,
      List<TextureKey> poisonTextures,
      TextureKey lightningTexture,
      List<TextureKey> noteTextures,
      List<TextureKey> starTextures,
      List<SpriteKey> woodFragments) {
    public EffectAssets {
      smokeTextures = copyNonEmpty(smokeTextures, "smokeTextures");
      damageSmokeTextures = copyNonEmpty(damageSmokeTextures, "damageSmokeTextures");
      poisonTextures = copyNonEmpty(poisonTextures, "poisonTextures");
      Objects.requireNonNull(lightningTexture, "lightningTexture");
      noteTextures = copyNonEmpty(noteTextures, "noteTextures");
      starTextures = copyNonEmpty(starTextures, "starTextures");
      woodFragments = copyNonEmpty(woodFragments, "woodFragments");
    }
  }

  /// Visual sound and cluck emoji sprite keys.
  public record EmojiAssets(Map<EmojiType, SpriteKey> sprites, List<SpriteKey> chickenCluckSprites) {
    public EmojiAssets {
      Objects.requireNonNull(sprites, "sprites");
      EnumMap<EmojiType, SpriteKey> copy = new EnumMap<>(EmojiType.class);
      sprites.forEach((type, sprite) ->
          copy.put(Objects.requireNonNull(type, "emoji type"), Objects.requireNonNull(sprite, "emoji sprite")));
      sprites = Collections.unmodifiableMap(copy);
      chickenCluckSprites = List.copyOf(Objects.requireNonNull(chickenCluckSprites, "chickenCluckSprites"));
    }
  }

  /// Full in-game visual assets extending the base landscape set.
  public record GameAssets(
      Map<Race, RaceVisuals> races, EffectAssets effects, List<SpriteKey> treasures, EmojiAssets emojis) {
    public GameAssets {
      races = copyCompleteEnumMap(Race.class, races, "races");
      Objects.requireNonNull(effects, "effects");
      treasures = copyNonEmpty(treasures, "treasures");
      Objects.requireNonNull(emojis, "emojis");
    }
  }

  private final LandscapeAssets landscape;
  private final @Nullable GameAssets game;

  private AssetRegistry(LandscapeAssets landscape, @Nullable GameAssets game) {
    this.landscape = Objects.requireNonNull(landscape, "landscape");
    this.game = game;
  }

  public static AssetRegistry forLandscape(LandscapeAssets landscape) {
    return new AssetRegistry(landscape, null);
  }

  public static AssetRegistry forGame(LandscapeAssets landscape, GameAssets game) {
    return new AssetRegistry(landscape, Objects.requireNonNull(game, "game"));
  }

  private static <T> List<T> copyNonEmpty(List<T> list, String name) {
    Objects.requireNonNull(list, name);
    List<T> copy = List.copyOf(list);
    if (copy.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be empty");
    }
    return copy;
  }

  private static <K extends Enum<K>, V> Map<K, V> copyCompleteEnumMap(
      Class<K> enumType, Map<K, V> source, String name) {
    Objects.requireNonNull(source, name);
    EnumMap<K, V> copy = new EnumMap<>(enumType);
    for (K key : enumType.getEnumConstants()) {
      V value = source.get(key);
      if (value == null) {
        throw new IllegalArgumentException(name + " missing entry for " + key);
      }
      copy.put(key, value);
    }
    return Collections.unmodifiableMap(copy);
  }

  private GameAssets requireGameAssets() {
    if (game == null) {
      throw new IllegalStateException("Game assets not loaded in this AssetRegistry");
    }
    return game;
  }

  private RaceVisuals requireRaceVisuals(Race race) {
    Objects.requireNonNull(race, "race");
    return Objects.requireNonNull(requireGameAssets().races().get(race));
  }

  public SpriteKey getWeaponSprite(Race race, WeaponTier tier) {
    Objects.requireNonNull(tier, "tier");
    return Objects.requireNonNull(requireRaceVisuals(race).weapons().get(tier));
  }

  public SpriteKey getRallyPoint(Race race) {
    return requireRaceVisuals(race).rallyPoint();
  }

  public SpriteKey getUnitSprite(Race race, UnitType type) {
    Objects.requireNonNull(type, "type");
    return Objects.requireNonNull(requireRaceVisuals(race).units().get(type));
  }

  public BuildingVisuals getBuildingVisuals(Race race, BuildingType type) {
    Objects.requireNonNull(type, "type");
    return Objects.requireNonNull(requireRaceVisuals(race).buildings().get(type));
  }

  public Optional<SpriteKey> getEmojiSprite(EmojiType type) {
    Objects.requireNonNull(type, "type");
    if (game == null) {
      return Optional.empty();
    }
    EmojiAssets emojis = game.emojis();
    if (type == EmojiType.CHICKEN_CLUCK && !emojis.chickenCluckSprites().isEmpty()) {
      List<SpriteKey> clucks = emojis.chickenCluckSprites();
      return Optional.of(clucks.get(ThreadLocalRandom.current().nextInt(clucks.size())));
    }
    return Optional.ofNullable(emojis.sprites().get(type));
  }

  public ShadowListKey<SelectableShadowRenderer> getDefaultShadow() {
    return landscape.defaultShadow();
  }

  public SpriteKey getCarriedSupplySprite(Race race, SupplyType type) {
    Objects.requireNonNull(type, "type");
    return Objects.requireNonNull(requireRaceVisuals(race).carriedSupplies().get(type));
  }

  public Map<SupplyType, SpriteKey> getCarriedSupplySprites(Race race) {
    return requireRaceVisuals(race).carriedSupplies();
  }

  public TextureKey[] getSmokeTextures() {
    return requireGameAssets().effects().smokeTextures().toArray(TextureKey[]::new);
  }

  public TextureKey[] getDamageSmokeTextures() {
    return requireGameAssets().effects().damageSmokeTextures().toArray(TextureKey[]::new);
  }

  public TextureKey[] getPoisonTextures() {
    return requireGameAssets().effects().poisonTextures().toArray(TextureKey[]::new);
  }

  public TextureKey getLightningTexture() {
    return requireGameAssets().effects().lightningTexture();
  }

  public TextureKey[] getNoteTextures() {
    return requireGameAssets().effects().noteTextures().toArray(TextureKey[]::new);
  }

  public TextureKey[] getStarTextures() {
    return requireGameAssets().effects().starTextures().toArray(TextureKey[]::new);
  }

  public SpriteKey[] getWoodFragments() {
    return requireGameAssets().effects().woodFragments().toArray(SpriteKey[]::new);
  }

  public List<SpriteKey> getTreasures() {
    return requireGameAssets().treasures();
  }

  public SpriteKey getRockFragmentSprite(int index) {
    List<SpriteKey> fragments = landscape.rockFragments();
    return fragments.get(Math.floorMod(index, fragments.size()));
  }

  public SpriteKey getIronFragmentSprite(int index) {
    List<SpriteKey> fragments = landscape.ironFragments();
    return fragments.get(Math.floorMod(index, fragments.size()));
  }

  public SpriteKey getPlantSprite(Terrain terrain, int index) {
    Objects.requireNonNull(terrain, "terrain");
    List<SpriteKey> sprites = Objects.requireNonNull(landscape.plantSprites().get(terrain));
    return sprites.get(Math.floorMod(index, sprites.size()));
  }

  public SpriteKey getChickenSprite() {
    return landscape.chickenSprite();
  }

  public SpriteKey getScenerySprite(SceneryModel scenery) {
    return switch (scenery) {
      case StatueScenery statue -> getTreasures().get(statue.getTreasureIndex());
      case CaptiveScenery captive -> getUnitSprite(captive.getRace(), captive.getUnitType());
      case GodScenery god -> getUnitSprite(god.getRace(), UnitType.CHIEFTAIN);
      case RallyPointScenery rally -> getRallyPoint(rally.getRace());
      case Plants plants -> getPlantSprite(plants.getTerrain(), plants.getIndex());
    };
  }

  public ShadowListKey<SelectableShadowRenderer> getSelectableShadow(Selectable<?> selectable) {
    Race race = selectable.getOwnerNoCheck().getRaceInfo().getRaceType();
    return switch (selectable) {
      case Unit _ -> getDefaultShadow();
      case Building building -> getBuildingVisuals(race, building.getTemplate().getBuildingType()).shadow();
    };
  }

  public SpriteKey getSupplySprite(SupplyModel supply) {
    return switch (supply) {
      case RockSupply rock -> getRockFragmentSprite(rock.getFragmentIndex());
      case IronSupply iron -> getIronFragmentSprite(iron.getFragmentIndex());
      case RubberSupply _ -> getChickenSprite();
    };
  }
}
