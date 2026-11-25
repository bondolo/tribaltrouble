package com.oddlabs.tt.client.render;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.engine.render.ShadowListKey;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.RubberGroup;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.tt.simulation.model.weapon.LightningCloud;
import com.oddlabs.tt.simulation.model.weapon.PoisonFog;
import com.oddlabs.tt.simulation.model.weapon.SonicBlast;
import com.oddlabs.tt.simulation.model.weapon.Stun;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class VisualModelFactoryTest {

  private final AtomicInteger nextKeyId = new AtomicInteger(10);
  private World world;
  private SpriteKey rockSprite;
  private SpriteKey ironSprite;
  private SpriteKey chickenSprite;
  private AssetRegistry.LandscapeAssets landscapeAssets;
  private VisualModelFactory factory;

  @BeforeEach
  void setUp() {
    int gridSize = 32;
    Landscape landscape = FlatLandscape.create(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerInfo> playerInfos = List.of(new PlayerInfo(0, Race.VIKINGS, "Player"));
    Color.Linear[] playerColors = new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f)};
    this.world = new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));
    this.rockSprite = new SpriteKey(1, new BoundingBox[0], new AnimationInfo.AnimationType[0]);
    this.ironSprite = new SpriteKey(2, new BoundingBox[0], new AnimationInfo.AnimationType[0]);
    this.chickenSprite = new SpriteKey(3, new BoundingBox[0], new AnimationInfo.AnimationType[0]);
    SpriteKey plantSprite = new SpriteKey(4, new BoundingBox[0], new AnimationInfo.AnimationType[0]);
    this.landscapeAssets =
        new AssetRegistry.LandscapeAssets(
            List.of(rockSprite),
            List.of(ironSprite),
            Map.of(Terrain.NATIVE, List.of(plantSprite), Terrain.VIKING, List.of(plantSprite)),
            chickenSprite,
            new ShadowListKey(0));
    this.factory = new VisualModelFactory(createDummyAudio(), AssetRegistry.forLandscape(landscapeAssets));
  }

  private SpriteKey newSpriteKey() {
    return new SpriteKey(nextKeyId.getAndIncrement(), new BoundingBox[0], new AnimationInfo.AnimationType[0]);
  }

  private TextureKey newTextureKey() {
    return new TextureKey(nextKeyId.getAndIncrement());
  }

  private ShadowListKey newShadowKey() {
    return new ShadowListKey(nextKeyId.getAndIncrement());
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

  private AssetRegistry.GameAssets createGameAssets() {
    EnumMap<Race, AssetRegistry.RaceVisuals> races = new EnumMap<>(Race.class);
    for (Race race : Race.values()) {
      races.put(race, createRaceVisuals());
    }
    AssetRegistry.EffectAssets effects =
        new AssetRegistry.EffectAssets(
            List.of(newTextureKey()),
            List.of(newTextureKey()),
            List.of(newTextureKey()),
            newTextureKey(),
            List.of(newTextureKey(), newTextureKey()),
            List.of(newTextureKey()),
            List.of(newSpriteKey(), newSpriteKey()));
    return new AssetRegistry.GameAssets(
        races, effects, List.of(newSpriteKey()), new AssetRegistry.EmojiAssets(Map.of(), List.of()));
  }

  private static AudioImplementation createDummyAudio() {
    return (AudioImplementation)
        Proxy.newProxyInstance(
            AudioImplementation.class.getClassLoader(), new Class<?>[] {AudioImplementation.class}, (_, _, _) -> null);
  }

  private static class DummyModel extends Model {
    DummyModel(World world) {
      super(world);
    }

    @Override
    protected BoundingBox @Nullable [] getLocalBounds() {
      return null;
    }
  }

  @Test
  void unknownModelResolvesToDynamicVisualModel() {
    DummyModel model = new DummyModel(world);

    VisualModel vm = factory.create(model);
    assertNotNull(vm);
    assertInstanceOf(DynamicVisualModel.class, vm);
    assertSame(model, vm.getModel());
  }

  @Test
  void supplyModelsResolveWithLandscapeOnlyAssets() {
    RockSupply rock = new RockSupply(world, 5, 5, 10f, 10f, false);
    IronSupply iron = new IronSupply(world, 6, 6, 12f, 12f, false);
    RubberSupply rubber = new RubberSupply(world, 4, 4, 8f, 8f, new RubberGroup(world), 6f, 6f);

    RockSupplyVisualModel rockVm = assertInstanceOf(RockSupplyVisualModel.class, factory.create(rock));
    assertSame(rockSprite, rockVm.getSpriteKey());

    IronSupplyVisualModel ironVm = assertInstanceOf(IronSupplyVisualModel.class, factory.create(iron));
    assertSame(ironSprite, ironVm.getSpriteKey());

    GenericSupplyVisualModel rubberVm = assertInstanceOf(GenericSupplyVisualModel.class, factory.create(rubber));
    assertSame(chickenSprite, rubberVm.getSpriteKey());
  }

  @Test
  void modelDirectClientModelBinding() {
    DummyModel model = new DummyModel(world);
    assertNull(model.getClientModel());

    VisualModel vm = factory.create(model);
    model.setClientModel(vm);
    assertSame(vm, model.getClientModel());

    model.setClientModel(null);
    assertNull(model.getClientModel());
  }

  @Test
  void gameModelsResolveUnitsBuildingsAndSpells() {
    AssetRegistry gameAssets = AssetRegistry.forGame(landscapeAssets, createGameAssets());
    VisualModelFactory gameFactory = new VisualModelFactory(createDummyAudio(), gameAssets);
    Player player = world.getPlayers().getFirst();

    Unit peon =
        new Unit(
            player,
            UnitGrid.coordinateFromGrid(10),
            UnitGrid.coordinateFromGrid(10),
            null,
            player.getRaceInfo().getUnitTemplate(UnitType.PEON));
    UnitVisualModel peonVm = assertInstanceOf(UnitVisualModel.class, gameFactory.create(peon));
    assertTrue(peonVm.getAccessories().stream().anyMatch(CarriedResourceAccessory.class::isInstance));

    Unit warrior =
        new Unit(
            player,
            UnitGrid.coordinateFromGrid(12),
            UnitGrid.coordinateFromGrid(12),
            null,
            player.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
    UnitVisualModel warriorVm = assertInstanceOf(UnitVisualModel.class, gameFactory.create(warrior));
    assertTrue(warriorVm.getAccessories().stream().anyMatch(WeaponAccessory.class::isInstance));

    Building building = new Building(player, player.getRaceInfo().getBuildingTemplate(BuildingType.QUARTERS), 20, 20);
    assertInstanceOf(BuildingVisualModel.class, gameFactory.create(building));

    Stun stun = new Stun(0f, 0f, 0f, 4f, 5f, 1f, peon);
    assertInstanceOf(StunVisualModel.class, gameFactory.create(stun));

    PoisonFog fog = new PoisonFog(0f, 0f, 0f, 4f, 0.5f, 0.5f, 5f, 1, peon);
    assertInstanceOf(PoisonFogVisualModel.class, gameFactory.create(fog));

    SonicBlast blast = new SonicBlast(0f, 0f, 0f, 4f, 0.5f, 0.2f, 10, 5, 2f, peon);
    assertInstanceOf(SonicBlastVisualModel.class, gameFactory.create(blast));

    LightningCloud cloud = new LightningCloud(world, 0f, 0f, 0f, 5f, 0.5f, 1f, 2f, 0.5f, 10, 10f, peon);
    assertInstanceOf(LightningCloudVisualModel.class, gameFactory.create(cloud));
  }
}
