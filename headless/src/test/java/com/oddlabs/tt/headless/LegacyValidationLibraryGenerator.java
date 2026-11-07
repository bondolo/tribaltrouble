package com.oddlabs.tt.headless;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility tool to regenerate {@code legacy_validation_library.csv} matching upstream baseline simulation outcomes.
 */
final class LegacyValidationLibraryGenerator {

    record RunInput(
                    int lineNumber,
                    int runId,
                    int seed,
                    int metersPerWorld,
                    Terrain terrain,
                    float hills,
                    float vegetation,
                    float supplies,
                    int maxTicks,
                    String playersStr
    ) {
    }

    record RunOutput(
                     RunInput input,
                     int winningTeam,
                     int finishTick,
                     int checksum
    ) {
        String toCsvLine() {
            return String.format(java.util.Locale.ROOT,
                    "%d,%d,%d,%s,%.2f,%.2f,%.2f,%d,%s,%d,%d,%d",
                    input.runId(),
                    input.seed(),
                    input.metersPerWorld(),
                    input.terrain(),
                    input.hills(),
                    input.vegetation(),
                    input.supplies(),
                    input.maxTicks(),
                    input.playersStr(),
                    winningTeam,
                    finishTick,
                    checksum
            );
        }
    }

    public static void main(String[] args) throws Exception {
        new LegacyValidationLibraryGenerator().regenerateCsv();
    }

    @Test
    @Disabled("Execute manually when regenerating legacy validation library")
    void runGenerator() throws Exception {
        regenerateCsv();
    }

    void regenerateCsv() throws Exception {
        System.setProperty("com.oddlabs.tt.legacy.deterministic", "true");
        List<RunInput> inputs = loadInputs();
        System.out.println("Loaded " + inputs.size() + " run configurations. Running simulation...");

        int total = inputs.size();
        AtomicInteger completed = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        RunOutput[] outputs = new RunOutput[total];

        inputs.parallelStream().forEach(input -> {
            HeadlessMatchConfig config = toMatchConfig(input);
            LegacyValidationMatchRunner runner = new LegacyValidationMatchRunner();
            HeadlessMatchResult result = runner.run(config);
            outputs[input.runId()] = new RunOutput(
                    input,
                    result.winningTeam(),
                    result.finalTick(),
                    result.finalChecksum()
            );

            int done = completed.incrementAndGet();
            if (done % 50 == 0 || done == total) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("Progress: %d / %d (%.1f%%) in %.1fs%n",
                        done, total, (done * 100.0) / total, elapsed / 1000.0);
            }
        });

        Path targetCsv = Path.of("headless/src/test/resources/legacy_validation_library.csv").toAbsolutePath();
        Path tempCsv = targetCsv.resolveSibling("legacy_validation_library.csv.tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(tempCsv, StandardCharsets.UTF_8)) {
            writer.write(
                    "run_id,seed,meters_per_world,terrain,hills,vegetation,supplies,max_ticks,players,winning_team,finish_tick,world_checksum");
            writer.newLine();
            for (RunOutput output : outputs) {
                writer.write(output.toCsvLine());
                writer.newLine();
            }
        }

        Files.move(tempCsv, targetCsv, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Successfully regenerated " + targetCsv);
    }

    private static HeadlessMatchConfig toMatchConfig(RunInput input) {
        IslandConfig islandConfig = new IslandConfig(
                input.terrain(),
                input.metersPerWorld(),
                input.hills(),
                input.vegetation(),
                input.supplies(),
                input.seed()
        );

        WorldParameters worldParameters = new WorldParameters(
                2,
                "MAP",
                Player.INITIAL_UNIT_COUNT,
                100
        );

        String[] playerTokens = input.playersStr().split(";");
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
                input.maxTicks(),
                50
        );
    }

    private static List<RunInput> loadInputs() throws Exception {
        List<RunInput> list = new ArrayList<>();
        try (InputStream in = LegacyValidationLibraryGenerator.class.getResourceAsStream(
                "/legacy_validation_library.csv")) {
            Objects.requireNonNull(in, "legacy_validation_library.csv not found in test resources");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                int lineNumber = 1;
                reader.readLine(); // header
                String line;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] cols = line.split(",");
                    list.add(new RunInput(
                            lineNumber,
                            Integer.parseInt(cols[0]),
                            Integer.parseInt(cols[1]),
                            Integer.parseInt(cols[2]),
                            Terrain.valueOf(cols[3].toUpperCase()),
                            Float.parseFloat(cols[4]),
                            Float.parseFloat(cols[5]),
                            Float.parseFloat(cols[6]),
                            Integer.parseInt(cols[7]),
                            cols[8]
                    ));
                }
            }
        }
        return list;
    }
}
