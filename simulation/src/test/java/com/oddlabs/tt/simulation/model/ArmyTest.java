package com.oddlabs.tt.simulation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Tests filtering and ability detection on {@link Army}.
class ArmyTest {

  private Player player;

  @BeforeEach
  void setUp() {
    int gridSize = 32;
    Landscape landscape = FlatLandscape.create(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);

    List<PlayerInfo> playerInfos =
        List.of(new PlayerInfo(0, Race.VIKINGS, "VikingPlayer"), new PlayerInfo(1, Race.NATIVES, "NativePlayer"));

    Color.Linear[] playerColors =
        new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f), new Color.Linear(0f, 0f, 1f, 1f)};

    World world = new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));

    player = world.getPlayers().getFirst();
  }

  @Test
  void testEmptyArmy() {
    Army army = new Army();
    assertEquals(0, army.size());
    assertTrue(army.isEmpty());
    assertFalse(army.containsAbility(Abilities.BUILD));
    assertFalse(army.containsAbility(Abilities.ATTACK));

    List<Selectable<?>> filtered = army.filter(Abilities.BUILD);
    assertEquals(0, filtered.size());
    assertTrue(filtered.isEmpty());
    assertSame(filtered, army.filter(Abilities.ATTACK), "Empty filter should return shared empty list");
  }

  @Test
  void testFilteringAndAbilityChecks() {
    Army army = new Army();

    Unit peon = new Unit(player, 10, 10, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
    Unit warrior = new Unit(player, 11, 11, null, player.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));

    army.add(peon);
    army.add(warrior);

    assertEquals(2, army.size());
    assertFalse(army.isEmpty());
    assertTrue(army.contains(peon));
    assertTrue(army.contains(warrior));

    assertTrue(army.containsAbility(Abilities.BUILD));
    assertTrue(army.containsAbility(Abilities.ATTACK));
    assertFalse(army.containsAbility(Abilities.MAGIC));

    List<Selectable<?>> builders = army.filter(Abilities.BUILD);
    assertEquals(1, builders.size());
    assertSame(peon, builders.getFirst());

    List<Selectable<?>> throwers = army.filter(Abilities.THROW);
    assertEquals(1, throwers.size());
    assertSame(warrior, throwers.getFirst());

    List<Selectable<?>> attackers = army.filter(Abilities.ATTACK);
    assertEquals(2, attackers.size());

    List<Selectable<?>> all = army.filter(Abilities.NONE);
    assertEquals(2, all.size());

    List<Selectable<?>> magic = army.filter(Abilities.MAGIC);
    assertEquals(0, magic.size());
    assertTrue(magic.isEmpty());
  }

  @Test
  void testEncapsulationAndIteration() {
    Army army = new Army();
    Unit peon = new Unit(player, 10, 10, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
    Unit warrior = new Unit(player, 11, 11, null, player.getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
    army.addAll(List.of(peon, warrior));

    List<Selectable<?>> iterated = new ArrayList<>();
    for (Selectable<?> s : army) {
      iterated.add(s);
    }
    assertEquals(List.of(peon, warrior), iterated);
    assertEquals(2, army.stream().count());

    assertThrows(UnsupportedOperationException.class, () -> army.getSet().add(peon));
    assertThrows(UnsupportedOperationException.class, () -> army.filter(Abilities.BUILD).add(peon));
  }
}
