package com.oddlabs.tt.headless;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.net.JitterConfig;
import com.oddlabs.net.JitterTimeManager;
import com.oddlabs.net.TickTimeManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying complete simulation runs to victory across procedural maps and teams.
 */
@Tag("integration")
class FullGameSimulationTest {

    @Test
    void test1v1BattleRunToVictory() throws Exception {
        int seed = new Random().nextInt();
        IslandConfig islandConfig = new IslandConfig(
                Terrain.NATIVE,
                256 * HeightMap.METERS_PER_UNIT_GRID,
                0.5f,
                0.5f,
                0.5f,
                seed
        );

        WorldParameters worldParameters = new WorldParameters(
                1,
                "headless_1v1_" + seed,
                Player.INITIAL_UNIT_COUNT,
                100
        );

        UnitInfo battleUnits = new UnitInfo(true, true, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

        List<HeadlessMatchConfig.PlayerConfig> players = List.of(
                new HeadlessMatchConfig.PlayerConfig(0, Race.VIKINGS, PlayerSlot.AI_HARD, "VikingArmy", battleUnits),
                new HeadlessMatchConfig.PlayerConfig(1, Race.NATIVES, PlayerSlot.AI_HARD, "NativeArmy", battleUnits)
        );

        HeadlessMatchConfig config = new HeadlessMatchConfig(
                islandConfig,
                worldParameters,
                players,
                150_000,
                50
        );

        HeadlessMatchRunner runner = new HeadlessMatchRunner();
        HeadlessMatchResult result = runner.run(config);

        assertTrue(result.victory(), "Match should conclude in victory");
        assertTrue(result.winningTeam() == 0 || result.winningTeam() == 1, "Winning team must be valid");
        assertFalse(result.survivingPlayerIndices().isEmpty(), "Winning team must have surviving players");
        for (int playerIndex : result.survivingPlayerIndices()) {
            assertEquals(result.winningTeam(), players.get(playerIndex).team(),
                    "All surviving players must belong to the winning team");
        }
    }

    @Test
    void test1v1BattleWithJitterClockRunToVictory() throws Exception {
        int seed = new Random().nextInt();
        IslandConfig islandConfig = new IslandConfig(
                Terrain.NATIVE,
                256 * HeightMap.METERS_PER_UNIT_GRID,
                0.5f,
                0.5f,
                0.5f,
                seed
        );

        WorldParameters worldParameters = new WorldParameters(
                1,
                "headless_jitter_1v1_" + seed,
                Player.INITIAL_UNIT_COUNT,
                100
        );

        UnitInfo battleUnits = new UnitInfo(true, true, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

        List<HeadlessMatchConfig.PlayerConfig> players = List.of(
                new HeadlessMatchConfig.PlayerConfig(0, Race.VIKINGS, PlayerSlot.AI_HARD, "VikingArmy", battleUnits),
                new HeadlessMatchConfig.PlayerConfig(1, Race.NATIVES, PlayerSlot.AI_HARD, "NativeArmy", battleUnits)
        );

        TickTimeManager baseClock = new TickTimeManager();
        JitterConfig jitterConfig = JitterConfig.mild(baseClock, seed);
        JitterTimeManager jitterClock = new JitterTimeManager(jitterConfig);

        HeadlessMatchConfig config = new HeadlessMatchConfig(
                islandConfig,
                worldParameters,
                players,
                300_000,
                50,
                jitterClock
        );

        HeadlessMatchRunner runner = new HeadlessMatchRunner();
        HeadlessMatchResult result = runner.run(config);

        assertTrue(result.victory(), "Match under jitter clock should conclude in victory");
        assertTrue(result.winningTeam() == 0 || result.winningTeam() == 1, "Winning team must be valid");
        assertFalse(result.survivingPlayerIndices().isEmpty(), "Winning team must have surviving players");
        for (int playerIndex : result.survivingPlayerIndices()) {
            assertEquals(result.winningTeam(), players.get(playerIndex).team(),
                    "All surviving players must belong to the winning team");
        }
    }

    @Test
    void testMultiPlayerTeamBattleRunToVictory() throws Exception {
        int seed = new Random().nextInt();
        IslandConfig islandConfig = new IslandConfig(
                Terrain.VIKING,
                256 * HeightMap.METERS_PER_UNIT_GRID,
                0.5f,
                0.5f,
                0.5f,
                seed
        );

        WorldParameters worldParameters = new WorldParameters(
                1,
                "headless_2v2_" + seed,
                Player.INITIAL_UNIT_COUNT,
                100
        );

        UnitInfo battleUnits = new UnitInfo(true, true, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

        List<HeadlessMatchConfig.PlayerConfig> players = List.of(
                new HeadlessMatchConfig.PlayerConfig(0, Race.VIKINGS, PlayerSlot.AI_HARD, "Team0-Viking", battleUnits),
                new HeadlessMatchConfig.PlayerConfig(0, Race.NATIVES, PlayerSlot.AI_HARD, "Team0-Native", battleUnits),
                new HeadlessMatchConfig.PlayerConfig(1, Race.VIKINGS, PlayerSlot.AI_HARD, "Team1-Viking", battleUnits),
                new HeadlessMatchConfig.PlayerConfig(1, Race.NATIVES, PlayerSlot.AI_HARD, "Team1-Native", battleUnits)
        );

        HeadlessMatchConfig config = new HeadlessMatchConfig(
                islandConfig,
                worldParameters,
                players,
                150_000,
                50
        );

        HeadlessMatchRunner runner = new HeadlessMatchRunner();
        HeadlessMatchResult result = runner.run(config);

        assertTrue(result.victory(), "Match should conclude in victory for one team");
        assertTrue(result.winningTeam() == 0 || result.winningTeam() == 1, "Winning team must be 0 or 1");
        assertFalse(result.survivingPlayerIndices().isEmpty(), "At least one player must survive on winning team");
        for (int playerIndex : result.survivingPlayerIndices()) {
            assertEquals(result.winningTeam(), players.get(playerIndex).team(),
                    "All surviving players must belong to the winning team");
        }
    }

    @Test
    void testProceduralMapWithRandomTerrainAndRacesRunToVictory() throws Exception {
        int seed = new Random().nextInt();
        Terrain terrain = seed % 2 == 0 ? Terrain.VIKING : Terrain.NATIVE;
        IslandConfig islandConfig = new IslandConfig(
                terrain,
                256 * HeightMap.METERS_PER_UNIT_GRID,
                0.5f,
                0.5f,
                0.5f,
                seed
        );

        WorldParameters worldParameters = new WorldParameters(
                1,
                "headless_procedural_" + seed,
                Player.INITIAL_UNIT_COUNT,
                100
        );

        UnitInfo battleUnits = new UnitInfo(true, true, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

        List<HeadlessMatchConfig.PlayerConfig> players = List.of(
                new HeadlessMatchConfig.PlayerConfig(0, Race.NATIVES, PlayerSlot.AI_HARD, "Native-Army", battleUnits),
                new HeadlessMatchConfig.PlayerConfig(1, Race.VIKINGS, PlayerSlot.AI_HARD, "Viking-Army", battleUnits)
        );

        HeadlessMatchConfig config = new HeadlessMatchConfig(
                islandConfig,
                worldParameters,
                players,
                300_000,
                50
        );

        HeadlessMatchRunner runner = new HeadlessMatchRunner();
        HeadlessMatchResult result = runner.run(config);

        assertTrue(result.victory(), "Match should conclude in victory");
        assertTrue(result.winningTeam() == 0 || result.winningTeam() == 1, "Winning team must be valid");
        assertFalse(result.survivingPlayerIndices().isEmpty(), "Surviving players list must not be empty");
        for (int playerIndex : result.survivingPlayerIndices()) {
            assertEquals(result.winningTeam(), players.get(playerIndex).team(),
                    "All surviving players must belong to the winning team");
        }
    }

    @Test
    void testMainParseMapKey() {
        // MapCode for a 2-player skirmish setup
        String testMapKey = "4Y4SDR388K";
        HeadlessMatchConfig config = Main.parseMapKey(testMapKey, 10_000);

        assertEquals(10_000, config.maxTicks());
        assertEquals(HeadlessMatchConfig.DEFAULT_CHECKSUM_INTERVAL, config.checksumIntervalTicks());
        assertFalse(config.players().isEmpty());
        assertTrue(config.players().size() >= 2);
        assertTrue(config.islandConfig().metersPerWorld() > 0);
    }
}
