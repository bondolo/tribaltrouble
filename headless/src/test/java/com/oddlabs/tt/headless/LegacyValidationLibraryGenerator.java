package com.oddlabs.tt.headless;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.AIDifficulty;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.UnitInfo;
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
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/// Utility tool to regenerate `legacy_validation_library.csv` matching upstream baseline simulation outcomes.
@NullMarked
final class LegacyValidationLibraryGenerator {

  record BaselineOutcome(int winningTeam, int finishTick, int worldChecksum) {}

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
      String playersStr,
      @Nullable BaselineOutcome expectedBaseline) {}

  record RunOutput(RunInput input, int winningTeam, int finishTick, int checksum, long merkleRootHash) {
    String toCsvLine() {
      return String.format(
          Locale.ROOT,
          "%d,%d,%d,%s,%.2f,%.2f,%.2f,%d,%s,%d,%d,%d,0x%016x",
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
          checksum,
          merkleRootHash);
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
    if (!AppConfig.LEGACY_DETERMINISTIC) {
      throw new IllegalStateException(
          "LegacyValidationLibraryGenerator must run with -Dcom.oddlabs.tt.legacy.deterministic=true");
    }
    List<RunInput> inputs = loadInputs();
    System.out.println("Loaded " + inputs.size() + " run configurations. Running simulation...");

    int total = inputs.size();
    AtomicInteger completed = new AtomicInteger(0);
    long startTime = System.currentTimeMillis();
    boolean allowBaselineChange = Boolean.getBoolean("legacy.allowBaselineChange");

    RunOutput[] outputs = new RunOutput[total];

    inputs.parallelStream()
        .forEach(input -> {
          HeadlessMatchConfig config = toMatchConfig(input);
          LegacyValidationMatchRunner runner = new LegacyValidationMatchRunner();
          HeadlessMatchResult result = runner.run(config);
          verifyBaselineParity(input, result, allowBaselineChange);
          outputs[input.runId()] =
              new RunOutput(
                  input,
                  result.winningTeam(),
                  result.finalTick(),
                  result.finalChecksum(),
                  result.finalMerkleRootHash());

          int done = completed.incrementAndGet();
          if (done % 50 == 0 || done == total) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.printf(
                "Progress: %d / %d (%.1f%%) in %.1fs%n", done, total, (done * 100.0) / total, elapsed / 1000.0);
          }
        });

    Path targetCsv = Path.of("headless/src/test/resources/legacy_validation_library.csv").toAbsolutePath();
    Path tempCsv = targetCsv.resolveSibling("legacy_validation_library.csv.tmp");

    try (BufferedWriter writer = Files.newBufferedWriter(tempCsv, StandardCharsets.UTF_8)) {
      writer.write(
          "run_id,seed,meters_per_world,terrain,hills,vegetation,supplies,max_ticks,players,winning_team,finish_tick,world_checksum,merkle_root_hash");
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
    IslandConfig islandConfig =
        new IslandConfig(
            input.terrain(), input.metersPerWorld(), input.hills(), input.vegetation(), input.supplies(), input.seed());

    WorldParameters worldParameters =
        new WorldParameters("MAP", Player.INITIAL_UNIT_COUNT, Player.DEFAULT_MAX_UNIT_COUNT);

    String[] playerTokens = input.playersStr().split(";");
    List<HeadlessMatchConfig.PlayerConfig> players = new ArrayList<>();
    UnitInfo unitInfo = new UnitInfo(false, false, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

    for (int i = 0; i < playerTokens.length; i++) {
      String[] parts = playerTokens[i].split(":");
      int team = Integer.parseInt(parts[0]);
      Race race = "VIKING".equalsIgnoreCase(parts[1]) ? Race.VIKINGS : Race.NATIVES;
      AIDifficulty aiDifficulty =
          switch (parts[2].toUpperCase()) {
            case "EASY" -> AIDifficulty.EASY;
            case "HARD" -> AIDifficulty.HARD;
            default -> AIDifficulty.NORMAL;
          };

      players.add(new HeadlessMatchConfig.PlayerConfig(team, race, aiDifficulty, "AI-" + i, unitInfo));
    }

    return new HeadlessMatchConfig(islandConfig, worldParameters, players, input.maxTicks(), 50);
  }

  private static List<RunInput> loadInputs() throws Exception {
    List<RunInput> list = new ArrayList<>();
    try (InputStream in =
        LegacyValidationLibraryGenerator.class.getResourceAsStream("/legacy_validation_library.csv")) {
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
          BaselineOutcome expectedBaseline =
              cols.length >= 12
                  ? new BaselineOutcome(
                      Integer.parseInt(cols[9]), Integer.parseInt(cols[10]), Integer.parseInt(cols[11]))
                  : null;
          list.add(
              new RunInput(
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
                  expectedBaseline));
        }
      }
    }
    return list;
  }

  static void verifyBaselineParity(RunInput input, HeadlessMatchResult result, boolean allowBaselineChange) {
    BaselineOutcome expected = input.expectedBaseline();
    if (expected == null || allowBaselineChange) {
      return;
    }
    if (expected.winningTeam() != result.winningTeam()
        || expected.finishTick() != result.finalTick()
        || expected.worldChecksum() != result.finalChecksum()) {
      throw new IllegalStateException(
          String.format(
              Locale.ROOT,
              "Legacy baseline drift on run %d (line %d): expected=(team=%d, tick=%d, checksum=%d) actual=(team=%d, tick=%d, checksum=%d). Pass -Dlegacy.allowBaselineChange=true to overwrite.",
              input.runId(),
              input.lineNumber(),
              expected.winningTeam(),
              expected.finishTick(),
              expected.worldChecksum(),
              result.winningTeam(),
              result.finalTick(),
              result.finalChecksum()));
    }
  }

  @Test
  void testVerifyBaselineParityDetectsDrift() {
    BaselineOutcome baseline = new BaselineOutcome(1, 20989, 194664523);
    RunInput input = new RunInput(2, 1, 492955, 256, Terrain.VIKING, 0.76f, 0.50f, 0.75f, 1_000_000, "", baseline);
    HeadlessMatchResult matching = new HeadlessMatchResult(1, true, 20989, 194664523, 0x1234L, List.of());
    HeadlessMatchResult drifted = new HeadlessMatchResult(1, true, 20990, 194664523, 0x1234L, List.of());

    assertDoesNotThrow(() -> verifyBaselineParity(input, matching, false));
    assertThrows(IllegalStateException.class, () -> verifyBaselineParity(input, drifted, false));
    assertDoesNotThrow(() -> verifyBaselineParity(input, drifted, true));
  }
}
