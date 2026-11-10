package com.oddlabs.tt.headless;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.AIDifficulty;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.UnitInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying bit-exact deterministic parity with legacy game execution.
 */
@Tag("integration")
@Execution(ExecutionMode.CONCURRENT)
class LegacyValidationSimulationTest {

    private static final int NUM_RANDOM_SCENARIOS = 10;

    static Stream<LegacyLibraryValidationSimulationTest.LegacyRunRecord> provideRandomLegacyScenarios()
            throws IOException {
        List<LegacyLibraryValidationSimulationTest.LegacyRunRecord> allRecords = LegacyLibraryValidationSimulationTest
                .loadLibrary();
        String seedProp = System.getProperty("legacy.random.seed");
        long seed = seedProp != null ? Long.parseLong(seedProp) : new Random().nextLong();
        System.out.println("Selecting " + NUM_RANDOM_SCENARIOS + " random legacy validation scenarios with seed "
                + seed);
        Random rng = new Random(seed);
        List<LegacyLibraryValidationSimulationTest.LegacyRunRecord> shuffled = new ArrayList<>(allRecords);
        Collections.shuffle(shuffled, rng);
        return shuffled.stream().limit(NUM_RANDOM_SCENARIOS);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRandomLegacyScenarios")
    @Execution(ExecutionMode.CONCURRENT)
    void testRandomLegacyScenarioFromCorpus(LegacyLibraryValidationSimulationTest.LegacyRunRecord record) {
        LegacyValidationMatchRunner runner = new LegacyValidationMatchRunner();
        LegacyLibraryValidationSimulationTest.runAndAssert(runner, record, false);
    }

    @Test
    void testLegacySeed42DirectExecutionParity() {
        runAndVerify(false);
    }

    @Test
    void testLegacySeed42ReifiedExecutionParity() {
        runAndVerify(true);
    }

    private void runAndVerify(boolean reifiedDispatch) {
        int seed = 42;
        IslandConfig islandConfig = new IslandConfig(
                Terrain.NATIVE,
                256,
                0.5f,
                0.5f,
                0.5f,
                seed
        );

        WorldParameters worldParameters = new WorldParameters(
                2,
                "42",
                Player.INITIAL_UNIT_COUNT,
                100
        );

        UnitInfo battleUnits = new UnitInfo(false, false, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

        List<HeadlessMatchConfig.PlayerConfig> players = List.of(
                new HeadlessMatchConfig.PlayerConfig(0, Race.NATIVES, AIDifficulty.NORMAL, "Player", battleUnits),
                new HeadlessMatchConfig.PlayerConfig(1, Race.VIKINGS, AIDifficulty.NORMAL, "Bot-Natives", battleUnits)
        );

        HeadlessMatchConfig config = new HeadlessMatchConfig(
                islandConfig,
                worldParameters,
                players,
                50_000,
                50
        );

        LegacyValidationMatchRunner runner = new LegacyValidationMatchRunner();
        HeadlessMatchResult result = runner.run(config, reifiedDispatch);

        System.out.println("Result (reified=" + reifiedDispatch + "): victory=" + result.victory()
                + ", winningTeam=" + result.winningTeam()
                + ", finishTick=" + result.finalTick()
                + ", checksum=" + result.finalChecksum());

        assertTrue(result.victory(), "Match should conclude in victory");
        assertEquals(0, result.winningTeam(), "Player (Team 0) must win");
        assertEquals(34_322, result.finalTick(), "Winning tick must match legacy baseline");
        assertEquals(232_946_378, result.finalChecksum(), "World checksum must match legacy baseline");
    }
}
