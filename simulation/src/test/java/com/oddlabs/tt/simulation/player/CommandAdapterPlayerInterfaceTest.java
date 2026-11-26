package com.oddlabs.tt.simulation.player;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.util.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandAdapterPlayerInterfaceTest {

  private List<PlayerCommand> emittedCommands;
  private CommandAdapterPlayerInterface adapter;
  private Player player;
  private Building quarters;
  private Unit chieftain;

  @BeforeEach
  void setUp() {
    emittedCommands = new ArrayList<>();
    adapter = new CommandAdapterPlayerInterface(emittedCommands::add);

    World world =
        new World(
            new WorldParameters("1", 5, 20),
            FlatLandscape.create(32, List.of(List.of(new SpawnLocation(10, 10)), List.of(new SpawnLocation(20, 20)))),
            List.of(new PlayerConfig(new PlayerInfo(0, Race.VIKINGS, "Viking"), new Color.Linear(1f, 0f, 0f, 1f))));
    player = world.getPlayers().getFirst();
    player.initializeStartingBase(new UnitInfo(true, true, 1, true, 1, 0, 0, 0));
    quarters =
        player.getUnits().getSet().stream()
            .filter(s -> s instanceof Building)
            .map(s -> (Building) s)
            .findFirst()
            .orElseThrow();
    chieftain = Objects.requireNonNull(player.getActiveChieftain());
  }

  @Test
  void testConstructorRequiresNonNullSink() {
    assertThrows(NullPointerException.class, () -> new CommandAdapterPlayerInterface(null));
  }

  @Test
  void testDeployUnits() {
    adapter.deployUnits(quarters, DeployType.PEON, 2);

    assertEquals(1, emittedCommands.size());
    PlayerCommand.DeployUnits cmd = assertInstanceOf(PlayerCommand.DeployUnits.class, emittedCommands.getFirst());
    assertEquals(quarters.getId(), cmd.buildingId());
    assertEquals(DeployType.PEON, cmd.deployType());
    assertEquals(2, cmd.numUnits());
  }

  @Test
  void testBuildWeapons() {
    adapter.buildWeapons(quarters, WeaponTier.ROCK, 3, true);
    adapter.buildWeapons(quarters, WeaponTier.IRON, 2, false);
    adapter.buildWeapons(quarters, WeaponTier.RUBBER, 1, true);

    assertEquals(3, emittedCommands.size());
    PlayerCommand.BuildWeapons rock = assertInstanceOf(PlayerCommand.BuildWeapons.class, emittedCommands.get(0));
    assertEquals(quarters.getId(), rock.buildingId());
    assertEquals(WeaponTier.ROCK, rock.tier());
    assertEquals(3, rock.numWeapons());
    assertEquals(true, rock.infinite());

    PlayerCommand.BuildWeapons iron = assertInstanceOf(PlayerCommand.BuildWeapons.class, emittedCommands.get(1));
    assertEquals(quarters.getId(), iron.buildingId());
    assertEquals(WeaponTier.IRON, iron.tier());
    assertEquals(2, iron.numWeapons());
    assertEquals(false, iron.infinite());

    PlayerCommand.BuildWeapons rubber = assertInstanceOf(PlayerCommand.BuildWeapons.class, emittedCommands.get(2));
    assertEquals(quarters.getId(), rubber.buildingId());
    assertEquals(WeaponTier.RUBBER, rubber.tier());
    assertEquals(1, rubber.numWeapons());
    assertEquals(true, rubber.infinite());
  }

  @Test
  void testDoMagic() {
    adapter.doMagic(chieftain, MagicType.LIGHTNING_CLOUD);

    assertEquals(1, emittedCommands.size());
    PlayerCommand.DoMagic cmd = assertInstanceOf(PlayerCommand.DoMagic.class, emittedCommands.getFirst());
    assertEquals(chieftain.getId(), cmd.chieftainId());
    assertEquals(MagicType.LIGHTNING_CLOUD, cmd.magic());
  }

  @Test
  void testExitTowerAndTrainChieftain() {
    adapter.exitTower(quarters);
    adapter.trainChieftain(quarters, true);

    assertEquals(2, emittedCommands.size());
    PlayerCommand.ExitTower exit = assertInstanceOf(PlayerCommand.ExitTower.class, emittedCommands.get(0));
    assertEquals(quarters.getId(), exit.buildingId());

    PlayerCommand.TrainChieftain train = assertInstanceOf(PlayerCommand.TrainChieftain.class, emittedCommands.get(1));
    assertEquals(quarters.getId(), train.buildingId());
    assertEquals(true, train.start());
  }

  @Test
  void testPlaceBuilding() {
    List<Selectable<?>> selection = List.of(chieftain);
    adapter.placeBuilding(selection, BuildingType.TOWER, 12, 14);

    assertEquals(1, emittedCommands.size());
    PlayerCommand.PlaceBuilding cmd = assertInstanceOf(PlayerCommand.PlaceBuilding.class, emittedCommands.getFirst());
    assertArrayEquals(new int[] {chieftain.getId()}, cmd.selectionIds());
    assertEquals(BuildingType.TOWER, cmd.templateType());
    assertEquals(12, cmd.gridX());
    assertEquals(14, cmd.gridY());
  }

  @Test
  void testSetRallyPoint() {
    adapter.setRallyPoint(quarters, 15, 20);
    adapter.setRallyPoint(quarters, new LandscapeTarget(16, 21));
    adapter.setRallyPoint(quarters, chieftain);

    assertEquals(3, emittedCommands.size());
    PlayerCommand.SetRallyPointCoords coords1 =
        assertInstanceOf(PlayerCommand.SetRallyPointCoords.class, emittedCommands.get(0));
    assertEquals(quarters.getId(), coords1.buildingId());
    assertEquals(15, coords1.gridX());
    assertEquals(20, coords1.gridY());

    PlayerCommand.SetRallyPointCoords coords2 =
        assertInstanceOf(PlayerCommand.SetRallyPointCoords.class, emittedCommands.get(1));
    assertEquals(quarters.getId(), coords2.buildingId());
    assertEquals(16, coords2.gridX());
    assertEquals(21, coords2.gridY());

    PlayerCommand.SetRallyPointTarget target =
        assertInstanceOf(PlayerCommand.SetRallyPointTarget.class, emittedCommands.get(2));
    assertEquals(quarters.getId(), target.buildingId());
    assertEquals(chieftain.getId(), target.targetEntityId());
  }

  @Test
  void testSetTargetAndLandscapeTarget() {
    adapter.setTarget(chieftain, quarters, Action.ATTACK, true);
    adapter.setTarget(List.of(chieftain), new LandscapeTarget(7, 8), Action.MOVE, false);
    adapter.setLandscapeTarget(chieftain, 9, 10, Action.MOVE, false);
    adapter.setLandscapeTarget(List.of(chieftain), 11, 12, Action.MOVE, true);

    assertEquals(4, emittedCommands.size());

    PlayerCommand.SetTarget targetCmd = assertInstanceOf(PlayerCommand.SetTarget.class, emittedCommands.get(0));
    assertArrayEquals(new int[] {chieftain.getId()}, targetCmd.selectionIds());
    assertEquals(quarters.getId(), targetCmd.targetEntityId());
    assertEquals(Action.ATTACK, targetCmd.action());
    assertEquals(true, targetCmd.aggressive());

    PlayerCommand.SetLandscapeTarget lt1 =
        assertInstanceOf(PlayerCommand.SetLandscapeTarget.class, emittedCommands.get(1));
    assertArrayEquals(new int[] {chieftain.getId()}, lt1.selectionIds());
    assertEquals(7, lt1.gridX());
    assertEquals(8, lt1.gridY());
    assertEquals(Action.MOVE, lt1.action());
    assertEquals(false, lt1.aggressive());

    PlayerCommand.SetLandscapeTarget lt2 =
        assertInstanceOf(PlayerCommand.SetLandscapeTarget.class, emittedCommands.get(2));
    assertArrayEquals(new int[] {chieftain.getId()}, lt2.selectionIds());
    assertEquals(9, lt2.gridX());
    assertEquals(10, lt2.gridY());

    PlayerCommand.SetLandscapeTarget lt3 =
        assertInstanceOf(PlayerCommand.SetLandscapeTarget.class, emittedCommands.get(3));
    assertArrayEquals(new int[] {chieftain.getId()}, lt3.selectionIds());
    assertEquals(11, lt3.gridX());
    assertEquals(12, lt3.gridY());
    assertEquals(true, lt3.aggressive());
  }

  @Test
  void testGamespeedCommands() {
    adapter.setPreferredGamespeed(3);
    adapter.changePreferredGamespeed(-1);

    assertEquals(2, emittedCommands.size());
    PlayerCommand.SetPreferredGamespeed setSpeed =
        assertInstanceOf(PlayerCommand.SetPreferredGamespeed.class, emittedCommands.get(0));
    assertEquals(3, setSpeed.speed());

    PlayerCommand.ChangePreferredGamespeed changeSpeed =
        assertInstanceOf(PlayerCommand.ChangePreferredGamespeed.class, emittedCommands.get(1));
    assertEquals(-1, changeSpeed.delta());
  }

  @Test
  void testEmptySelectionGeneratesEmptyIds() {
    adapter.placeBuilding(List.of(), BuildingType.TOWER, 5, 5);
    PlayerCommand.PlaceBuilding place = assertInstanceOf(PlayerCommand.PlaceBuilding.class, emittedCommands.getFirst());
    assertEquals(0, place.selectionIds().length);
  }

  @Test
  void testListOverloads() {
    adapter.placeBuilding(List.of(chieftain), BuildingType.TOWER, 3, 4);
    adapter.setTarget(List.of(chieftain), quarters, Action.ATTACK, true);
    adapter.setLandscapeTarget(List.of(chieftain), 5, 6, Action.MOVE, false);

    assertEquals(3, emittedCommands.size());
    PlayerCommand.PlaceBuilding place = assertInstanceOf(PlayerCommand.PlaceBuilding.class, emittedCommands.get(0));
    assertArrayEquals(new int[] {chieftain.getId()}, place.selectionIds());
    assertEquals(3, place.gridX());
    assertEquals(4, place.gridY());

    PlayerCommand.SetTarget target = assertInstanceOf(PlayerCommand.SetTarget.class, emittedCommands.get(1));
    assertArrayEquals(new int[] {chieftain.getId()}, target.selectionIds());
    assertEquals(quarters.getId(), target.targetEntityId());

    PlayerCommand.SetLandscapeTarget landscape =
        assertInstanceOf(PlayerCommand.SetLandscapeTarget.class, emittedCommands.get(2));
    assertArrayEquals(new int[] {chieftain.getId()}, landscape.selectionIds());
    assertEquals(5, landscape.gridX());
    assertEquals(6, landscape.gridY());
  }
}
