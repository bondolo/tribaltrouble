package com.oddlabs.tt.headless;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates simulation execution parity against the legacy simulation baseline library.
 */
@NullMarked
@Tag("integration")
@Tag("release")
@Execution(ExecutionMode.CONCURRENT)
public class LegacyLibraryValidationSimulationTest {

    record LegacyRunRecord(
                           int lineNumber,
                           int runId,
                           int seed,
                           int metersPerWorld,
                           Terrain terrain,
                           float hills,
                           float vegetation,
                           float supplies,
                           int maxTicks,
                           String playersStr,
                           int expectedWinningTeam,
                           int expectedFinishTick,
                           int expectedWorldChecksum
    ) {
        @Override
        public String toString() {
            return "Line " + lineNumber + ": Run " + runId + " (seed=" + seed + ", " + terrain + ", " + metersPerWorld
                    + "m)";
        }
    }

    static Stream<LegacyRunRecord> provideLegacyRuns() throws IOException {
        List<LegacyRunRecord> records = loadLibrary();
        String linesProp = System.getProperty("lines", System.getProperty("test.lines"));
        String runsProp = System.getProperty("runs", System.getProperty("test.runs"));

        return records.stream()
                .filter(r -> LegacyValidationMatchRunner.matchesFilter(r.lineNumber(), linesProp))
                .filter(r -> LegacyValidationMatchRunner.matchesFilter(r.runId(), runsProp));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLegacyRuns")
    @Execution(ExecutionMode.CONCURRENT)
    void validateLegacyRun(LegacyRunRecord record) {
        LegacyValidationMatchRunner runner = new LegacyValidationMatchRunner();
        runAndAssert(runner, record);
    }

    private void runAndAssert(LegacyValidationMatchRunner runner, LegacyRunRecord record) {
        HeadlessMatchConfig config = toMatchConfig(record);
        HeadlessMatchResult result = runner.run(config);

        assertEquals(record.expectedWinningTeam(), result.winningTeam(),
                "Run " + record.runId() + " winning team mismatch");
        assertEquals(record.expectedFinishTick(), result.finalTick(),
                "Run " + record.runId() + " finish tick mismatch");
        assertEquals(record.expectedWorldChecksum(), result.finalChecksum(),
                "Run " + record.runId() + " final checksum mismatch");
    }

    private HeadlessMatchConfig toMatchConfig(LegacyRunRecord record) {
        IslandConfig islandConfig = new IslandConfig(
                record.terrain(),
                record.metersPerWorld(),
                record.hills(),
                record.vegetation(),
                record.supplies(),
                record.seed()
        );

        WorldParameters worldParameters = new WorldParameters(
                2,
                "MAP",
                Player.INITIAL_UNIT_COUNT,
                100
        );

        String[] playerTokens = record.playersStr().split(";");
        List<HeadlessMatchConfig.PlayerConfig> players = new ArrayList<>();
        UnitInfo unitInfo = new UnitInfo(false, false, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

        for (int i = 0; i < playerTokens.length; i++) {
            String[] parts = playerTokens[i].split(":");
            int team = Integer.parseInt(parts[0]);
            Race race = "VIKING".equalsIgnoreCase(parts[1]) ? Race.VIKINGS : Race.NATIVES;
            int aiDifficulty = switch (parts[2].toUpperCase()) {
                case "EASY" -> PlayerSlot.AI_EASY;
                case "HARD" -> PlayerSlot.AI_HARD;
                default -> PlayerSlot.AI_NORMAL;
            };

            players.add(new HeadlessMatchConfig.PlayerConfig(
                    team,
                    race,
                    aiDifficulty,
                    "AI-" + i,
                    unitInfo
            ));
        }

        return new HeadlessMatchConfig(
                islandConfig,
                worldParameters,
                players,
                record.maxTicks(),
                50
        );
    }

    private static List<LegacyRunRecord> loadLibrary() throws IOException {
        List<LegacyRunRecord> list = new ArrayList<>();
        try (InputStream in = LegacyLibraryValidationSimulationTest.class.getResourceAsStream(
                "/legacy_validation_library.csv")) {
            Objects.requireNonNull(in, "legacy_validation_library.csv not found in test resources");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                int lineNumber = 1; // line 1 is header
                reader.readLine(); // skip header
                String line;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] cols = line.split(",");
                    list.add(new LegacyRunRecord(
                            lineNumber,
                            Integer.parseInt(cols[0]),
                            Integer.parseInt(cols[1]),
                            Integer.parseInt(cols[2]),
                            Terrain.valueOf(cols[3].toUpperCase()),
                            Float.parseFloat(cols[4]),
                            Float.parseFloat(cols[5]),
                            Float.parseFloat(cols[6]),
                            Integer.parseInt(cols[7]),
                            cols[8],
                            Integer.parseInt(cols[9]),
                            Integer.parseInt(cols[10]),
                            Integer.parseInt(cols[11])
                    ));
                }
            }
        }
        return list;
    }
}
