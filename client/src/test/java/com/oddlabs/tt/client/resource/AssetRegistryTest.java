package com.oddlabs.tt.client.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.client.render.SelectableShadowRenderer;
import com.oddlabs.tt.engine.render.ShadowListKey;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.CaptiveScenery;
import com.oddlabs.tt.simulation.model.GodScenery;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Plants;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RallyPointScenery;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.RubberGroup;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.StatueScenery;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class AssetRegistryTest {
  private final AtomicInteger nextKeyId = new AtomicInteger(1);

  private SpriteKey newSpriteKey() {
    return new SpriteKey(nextKeyId.getAndIncrement(), new BoundingBox[0], new AnimationInfo.AnimationType[0]);
  }

  private TextureKey newTextureKey() {
    return new TextureKey(nextKeyId.getAndIncrement());
  }

  private ShadowListKey<SelectableShadowRenderer> newShadowKey() {
    return new ShadowListKey<>(nextKeyId.getAndIncrement(), SelectableShadowRenderer.class);
  }

  private AssetRegistry.LandscapeAssets createLandscapeAssets() {
    List<SpriteKey> rocks = List.of(newSpriteKey(), newSpriteKey());
    List<SpriteKey> irons = List.of(newSpriteKey(), newSpriteKey(), newSpriteKey());
    Map<Terrain, List<SpriteKey>> plants =
        Map.of(
            Terrain.NATIVE, List.of(newSpriteKey(), newSpriteKey()),
            Terrain.VIKING, List.of(newSpriteKey(), newSpriteKey()));
    SpriteKey chicken = newSpriteKey();
    return new AssetRegistry.LandscapeAssets(rocks, irons, plants, chicken, newShadowKey());
  }

  private AssetRegistry.RaceVisuals createRaceVisuals() {
    EnumMap<UnitType, SpriteKey> units = new EnumMap<>(UnitType.class);
    for (UnitType type : UnitType.values()) {
      units.put(type, newSpriteKey());
    }
    EnumMap<BuildingType, AssetRegistry.BuildingVisuals> buildings = new EnumMap<>(BuildingType.class);
    for (BuildingType type : BuildingType.values()) {
      buildings.put(
          type,
          new AssetRegistry.BuildingVisuals(newSpriteKey(), newSpriteKey(), newSpriteKey(), newShadowKey(), 4.5f));
    }
    EnumMap<WeaponTier, SpriteKey> weapons = new EnumMap<>(WeaponTier.class);
    for (WeaponTier tier : WeaponTier.values()) {
      weapons.put(tier, newSpriteKey());
    }
    EnumMap<SupplyType, SpriteKey> carriedSupplies = new EnumMap<>(SupplyType.class);
    for (SupplyType supply : SupplyType.values()) {
      carriedSupplies.put(supply, newSpriteKey());
    }
    return new AssetRegistry.RaceVisuals(units, buildings, weapons, carriedSupplies, newSpriteKey());
  }

  private AssetRegistry.EffectAssets createEffectAssets() {
    return new AssetRegistry.EffectAssets(
        List.of(newTextureKey()),
        List.of(newTextureKey()),
        List.of(newTextureKey()),
        newTextureKey(),
        List.of(newTextureKey(), newTextureKey()),
        List.of(newTextureKey()),
        List.of(newSpriteKey(), newSpriteKey()));
  }

  private AssetRegistry.GameAssets createGameAssets(
      Map<EmojiType, SpriteKey> emojiSprites, List<SpriteKey> cluckSprites) {
    EnumMap<Race, AssetRegistry.RaceVisuals> races = new EnumMap<>(Race.class);
    for (Race race : Race.values()) {
      races.put(race, createRaceVisuals());
    }
    return new AssetRegistry.GameAssets(
        races,
        createEffectAssets(),
        List.of(newSpriteKey(), newSpriteKey()),
        new AssetRegistry.EmojiAssets(emojiSprites, cluckSprites));
  }

  private static World createTestWorld() {
    Landscape landscape = FlatLandscape.create(32);
    WorldParameters params = new WorldParameters("1", 5, 20);
    List<PlayerInfo> players = List.of(new PlayerInfo(0, Race.VIKINGS, "Player"));
    Color.Linear[] colors = new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f)};
    return new World(params, landscape, PlayerConfig.from(players, colors));
  }

  @Test
  void landscapeRegistryResolvesLandscapeAndRejectsGameAssets() {
    AssetRegistry.LandscapeAssets landscape = createLandscapeAssets();
    AssetRegistry registry = AssetRegistry.forLandscape(landscape);

    assertSame(landscape.rockFragments().get(0), registry.getRockFragmentSprite(0));
    assertSame(landscape.rockFragments().get(1), registry.getRockFragmentSprite(3));
    assertSame(landscape.rockFragments().get(1), registry.getRockFragmentSprite(-1));
    assertSame(landscape.ironFragments().get(2), registry.getIronFragmentSprite(2));
    assertSame(landscape.ironFragments().get(0), registry.getIronFragmentSprite(3));
    assertSame(landscape.plantSprites().get(Terrain.NATIVE).get(1), registry.getPlantSprite(Terrain.NATIVE, 3));
    assertSame(landscape.plantSprites().get(Terrain.VIKING).get(0), registry.getPlantSprite(Terrain.VIKING, 2));
    assertSame(landscape.chickenSprite(), registry.getChickenSprite());
    assertSame(landscape.defaultShadow(), registry.getDefaultShadow());

    for (EmojiType emoji : EmojiType.values()) {
      assertEquals(Optional.empty(), registry.getEmojiSprite(emoji));
    }

    World world = createTestWorld();
    Player player = world.getPlayers().getFirst();
    Plants plants = new Plants(world, 4f, 4f, 1f, 0f, Terrain.VIKING, 1);
    assertSame(landscape.plantSprites().get(Terrain.VIKING).get(1), registry.getScenerySprite(plants));
    RockSupply rock = new RockSupply(world, 5, 5, 10f, 10f, false);
    assertSame(registry.getRockFragmentSprite(rock.getFragmentIndex()), registry.getSupplySprite(rock));

    assertThrows(IllegalStateException.class, () -> registry.getUnitSprite(Race.VIKINGS, UnitType.PEON));
    assertThrows(IllegalStateException.class, () -> registry.getBuildingVisuals(Race.NATIVES, BuildingType.TOWER));
    assertThrows(IllegalStateException.class, () -> registry.getWeaponSprite(Race.VIKINGS, WeaponTier.ROCK));
    assertThrows(IllegalStateException.class, () -> registry.getRallyPoint(Race.NATIVES));
    assertThrows(IllegalStateException.class, () -> registry.getCarriedSupplySprite(Race.VIKINGS, SupplyType.WOOD));
    assertThrows(IllegalStateException.class, () -> registry.getCarriedSupplySprites(Race.VIKINGS));
    assertThrows(IllegalStateException.class, registry::getSmokeTextures);
    assertThrows(IllegalStateException.class, registry::getDamageSmokeTextures);
    assertThrows(IllegalStateException.class, registry::getPoisonTextures);
    assertThrows(IllegalStateException.class, registry::getLightningTexture);
    assertThrows(IllegalStateException.class, registry::getNoteTextures);
    assertThrows(IllegalStateException.class, registry::getStarTextures);
    assertThrows(IllegalStateException.class, registry::getWoodFragments);
    assertThrows(IllegalStateException.class, registry::getTreasures);

    Unit unit = new Unit(player, 16f, 16f, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
    Building building = new Building(player, player.getRaceInfo().getBuildingTemplate(BuildingType.QUARTERS), 10, 10);
    assertSame(landscape.defaultShadow(), registry.getSelectableShadow(unit));
    assertThrows(IllegalStateException.class, () -> registry.getSelectableShadow(building));
  }

  @Test
  void gameRegistryResolvesVisualsShadowsSuppliesSceneryAndCopies() {
    AssetRegistry.LandscapeAssets landscape = createLandscapeAssets();
    SpriteKey gravestone = newSpriteKey();
    SpriteKey cluck1 = newSpriteKey();
    SpriteKey cluck2 = newSpriteKey();
    List<SpriteKey> cluckSprites = List.of(cluck1, cluck2);
    AssetRegistry.GameAssets game = createGameAssets(Map.of(EmojiType.GRAVESTONE, gravestone), cluckSprites);
    AssetRegistry registry = AssetRegistry.forGame(landscape, game);

    for (Race race : Race.values()) {
      AssetRegistry.RaceVisuals raceVisuals = game.races().get(race);
      for (UnitType unitType : UnitType.values()) {
        assertSame(raceVisuals.units().get(unitType), registry.getUnitSprite(race, unitType));
      }
      for (BuildingType buildingType : BuildingType.values()) {
        assertSame(raceVisuals.buildings().get(buildingType), registry.getBuildingVisuals(race, buildingType));
      }
      for (WeaponTier tier : WeaponTier.values()) {
        assertSame(raceVisuals.weapons().get(tier), registry.getWeaponSprite(race, tier));
      }
      for (SupplyType supply : SupplyType.values()) {
        assertSame(raceVisuals.carriedSupplies().get(supply), registry.getCarriedSupplySprite(race, supply));
      }
      assertEquals(raceVisuals.carriedSupplies(), registry.getCarriedSupplySprites(race));
      assertThrows(
          UnsupportedOperationException.class,
          () -> registry.getCarriedSupplySprites(race).put(SupplyType.WOOD, newSpriteKey()));
      assertSame(raceVisuals.rallyPoint(), registry.getRallyPoint(race));
    }

    assertSame(landscape.defaultShadow(), registry.getDefaultShadow());
    assertSame(game.effects().lightningTexture(), registry.getLightningTexture());

    World world = createTestWorld();
    Player player = world.getPlayers().getFirst();
    Unit unit = new Unit(player, 16f, 16f, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
    Building building = new Building(player, player.getRaceInfo().getBuildingTemplate(BuildingType.QUARTERS), 10, 10);
    assertSame(landscape.defaultShadow(), registry.getSelectableShadow(unit));
    assertSame(
        game.races().get(Race.VIKINGS).buildings().get(BuildingType.QUARTERS).shadow(),
        registry.getSelectableShadow(building));

    RockSupply rock = new RockSupply(world, 5, 5, 10f, 10f, false);
    IronSupply iron = new IronSupply(world, 6, 6, 12f, 12f, false);
    RubberSupply rubber = new RubberSupply(world, 4, 4, 8f, 8f, new RubberGroup(world), 6f, 6f);
    assertSame(registry.getRockFragmentSprite(rock.getFragmentIndex()), registry.getSupplySprite(rock));
    assertSame(registry.getIronFragmentSprite(iron.getFragmentIndex()), registry.getSupplySprite(iron));
    assertSame(landscape.chickenSprite(), registry.getSupplySprite(rubber));

    BoundingBox[] bounds = new BoundingBox[] {new BoundingBox()};
    StatueScenery statue = new StatueScenery(world, 8f, 8f, 1f, 0f, 1, bounds, 2f, false, "statue");
    CaptiveScenery captive =
        new CaptiveScenery(world, 8f, 8f, 1f, 0f, Race.NATIVES, UnitType.PEON, bounds, 0f, false, null, 0, 1f, 0f);
    GodScenery god = new GodScenery(world, 8f, 8f, 1f, 0f, Race.VIKINGS, bounds, 0f, false, null, 0, 1f, 0f);
    RallyPointScenery rally = new RallyPointScenery(world, 8f, 8f, 1f, 0f, Race.NATIVES, bounds);
    Plants plants = new Plants(world, 8f, 8f, 1f, 0f, Terrain.NATIVE, 0);

    assertSame(game.treasures().get(1), registry.getScenerySprite(statue));
    assertSame(game.races().get(Race.NATIVES).units().get(UnitType.PEON), registry.getScenerySprite(captive));
    assertSame(game.races().get(Race.VIKINGS).units().get(UnitType.CHIEFTAIN), registry.getScenerySprite(god));
    assertSame(game.races().get(Race.NATIVES).rallyPoint(), registry.getScenerySprite(rally));
    assertSame(landscape.plantSprites().get(Terrain.NATIVE).get(0), registry.getScenerySprite(plants));

    TextureKey[] smokeCopy = registry.getSmokeTextures();
    TextureKey originalSmoke = smokeCopy[0];
    smokeCopy[0] = newTextureKey();
    assertArrayEquals(new TextureKey[] {originalSmoke}, registry.getSmokeTextures());

    TextureKey[] damageCopy = registry.getDamageSmokeTextures();
    TextureKey originalDamage = damageCopy[0];
    damageCopy[0] = newTextureKey();
    assertArrayEquals(new TextureKey[] {originalDamage}, registry.getDamageSmokeTextures());

    TextureKey[] poisonCopy = registry.getPoisonTextures();
    TextureKey originalPoison = poisonCopy[0];
    poisonCopy[0] = newTextureKey();
    assertArrayEquals(new TextureKey[] {originalPoison}, registry.getPoisonTextures());

    TextureKey[] notesCopy = registry.getNoteTextures();
    TextureKey originalNote0 = notesCopy[0];
    notesCopy[0] = newTextureKey();
    assertSame(originalNote0, registry.getNoteTextures()[0]);

    TextureKey[] starCopy = registry.getStarTextures();
    TextureKey originalStar = starCopy[0];
    starCopy[0] = newTextureKey();
    assertArrayEquals(new TextureKey[] {originalStar}, registry.getStarTextures());

    SpriteKey[] woodCopy = registry.getWoodFragments();
    SpriteKey originalWood0 = woodCopy[0];
    woodCopy[0] = newSpriteKey();
    assertSame(originalWood0, registry.getWoodFragments()[0]);

    List<SpriteKey> treasures = registry.getTreasures();
    assertEquals(2, treasures.size());
    assertThrows(UnsupportedOperationException.class, () -> treasures.add(newSpriteKey()));

    assertEquals(Optional.of(gravestone), registry.getEmojiSprite(EmojiType.GRAVESTONE));
    assertEquals(Optional.empty(), registry.getEmojiSprite(EmojiType.REPAIR_SAW));
    Optional<SpriteKey> cluck = registry.getEmojiSprite(EmojiType.CHICKEN_CLUCK);
    assertTrue(cluck.isPresent());
    assertTrue(cluckSprites.contains(cluck.orElseThrow()));

    AssetRegistry noCluckRegistry = AssetRegistry.forGame(landscape, createGameAssets(Map.of(), List.of()));
    assertEquals(Optional.empty(), noCluckRegistry.getEmojiSprite(EmojiType.CHICKEN_CLUCK));
  }

  @Test
  void recordConstructorsRejectEmptyListsAndIncompleteEnumMaps() {
    AssetRegistry.LandscapeAssets validLandscape = createLandscapeAssets();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.LandscapeAssets(
                List.of(),
                validLandscape.ironFragments(),
                validLandscape.plantSprites(),
                validLandscape.chickenSprite(),
                validLandscape.defaultShadow()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.LandscapeAssets(
                validLandscape.rockFragments(),
                List.of(),
                validLandscape.plantSprites(),
                validLandscape.chickenSprite(),
                validLandscape.defaultShadow()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.LandscapeAssets(
                validLandscape.rockFragments(),
                validLandscape.ironFragments(),
                Map.of(Terrain.NATIVE, List.of(newSpriteKey())),
                validLandscape.chickenSprite(),
                validLandscape.defaultShadow()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.LandscapeAssets(
                validLandscape.rockFragments(),
                validLandscape.ironFragments(),
                Map.of(Terrain.NATIVE, List.of(), Terrain.VIKING, List.of(newSpriteKey())),
                validLandscape.chickenSprite(),
                validLandscape.defaultShadow()));

    AssetRegistry.RaceVisuals validRace = createRaceVisuals();
    EnumMap<UnitType, SpriteKey> incompleteUnits = new EnumMap<>(validRace.units());
    incompleteUnits.remove(UnitType.PEON);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.RaceVisuals(
                incompleteUnits,
                validRace.buildings(),
                validRace.weapons(),
                validRace.carriedSupplies(),
                validRace.rallyPoint()));

    EnumMap<BuildingType, AssetRegistry.BuildingVisuals> incompleteBuildings = new EnumMap<>(validRace.buildings());
    incompleteBuildings.remove(BuildingType.TOWER);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.RaceVisuals(
                validRace.units(),
                incompleteBuildings,
                validRace.weapons(),
                validRace.carriedSupplies(),
                validRace.rallyPoint()));

    EnumMap<WeaponTier, SpriteKey> incompleteWeapons = new EnumMap<>(validRace.weapons());
    incompleteWeapons.remove(WeaponTier.IRON);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.RaceVisuals(
                validRace.units(),
                validRace.buildings(),
                incompleteWeapons,
                validRace.carriedSupplies(),
                validRace.rallyPoint()));

    EnumMap<SupplyType, SpriteKey> incompleteSupplies = new EnumMap<>(validRace.carriedSupplies());
    incompleteSupplies.remove(SupplyType.RUBBER);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.RaceVisuals(
                validRace.units(),
                validRace.buildings(),
                validRace.weapons(),
                incompleteSupplies,
                validRace.rallyPoint()));

    AssetRegistry.EffectAssets validEffects = createEffectAssets();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.EffectAssets(
                List.of(),
                validEffects.damageSmokeTextures(),
                validEffects.poisonTextures(),
                validEffects.lightningTexture(),
                validEffects.noteTextures(),
                validEffects.starTextures(),
                validEffects.woodFragments()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.EffectAssets(
                validEffects.smokeTextures(),
                List.of(),
                validEffects.poisonTextures(),
                validEffects.lightningTexture(),
                validEffects.noteTextures(),
                validEffects.starTextures(),
                validEffects.woodFragments()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.EffectAssets(
                validEffects.smokeTextures(),
                validEffects.damageSmokeTextures(),
                List.of(),
                validEffects.lightningTexture(),
                validEffects.noteTextures(),
                validEffects.starTextures(),
                validEffects.woodFragments()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.EffectAssets(
                validEffects.smokeTextures(),
                validEffects.damageSmokeTextures(),
                validEffects.poisonTextures(),
                validEffects.lightningTexture(),
                List.of(),
                validEffects.starTextures(),
                validEffects.woodFragments()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.EffectAssets(
                validEffects.smokeTextures(),
                validEffects.damageSmokeTextures(),
                validEffects.poisonTextures(),
                validEffects.lightningTexture(),
                validEffects.noteTextures(),
                List.of(),
                validEffects.woodFragments()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.EffectAssets(
                validEffects.smokeTextures(),
                validEffects.damageSmokeTextures(),
                validEffects.poisonTextures(),
                validEffects.lightningTexture(),
                validEffects.noteTextures(),
                validEffects.starTextures(),
                List.of()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.GameAssets(
                Map.of(Race.VIKINGS, validRace),
                validEffects,
                List.of(newSpriteKey()),
                new AssetRegistry.EmojiAssets(Map.of(), List.of())));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AssetRegistry.GameAssets(
                Map.of(Race.VIKINGS, validRace, Race.NATIVES, validRace),
                validEffects,
                List.of(),
                new AssetRegistry.EmojiAssets(Map.of(), List.of())));
  }

  @Test
  void audioRegistryReturnsStaticRaceAudioForAllRaces() {
    for (Race race : Race.values()) {
      assertNotNull(AudioRegistry.getAttackNotificationAudio(race));
      assertNotNull(AudioRegistry.getBuildingNotificationAudio(race));
      assertNotNull(AudioRegistry.getMusic(race));
    }
    assertSame(AudioRegistry.ATTACK_NOTIFY_NATIVE, AudioRegistry.getAttackNotificationAudio(Race.NATIVES));
    assertSame(AudioRegistry.BUILDING_NOTIFY_NATIVE, AudioRegistry.getBuildingNotificationAudio(Race.NATIVES));
    assertSame(AudioRegistry.MUSIC_NATIVE, AudioRegistry.getMusic(Race.NATIVES));
    assertSame(AudioRegistry.ATTACK_NOTIFY_VIKING, AudioRegistry.getAttackNotificationAudio(Race.VIKINGS));
    assertSame(AudioRegistry.BUILDING_NOTIFY_VIKING, AudioRegistry.getBuildingNotificationAudio(Race.VIKINGS));
    assertSame(AudioRegistry.MUSIC_VIKING, AudioRegistry.getMusic(Race.VIKINGS));
  }
}
