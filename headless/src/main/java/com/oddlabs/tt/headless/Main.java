package com.oddlabs.tt.headless;

import com.oddlabs.net.TickTimeManager;
import com.oddlabs.procedural.MapCode;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.player.AIDifficulty;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.UnitInfo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/// Standalone executable entry point for running headless match sessions.
public final class Main {

  private static final int[] SIZES = new int[] {256, 512, 1024};
  private static final int SLIDER_MAX_VALUE = 10;

  private Main() {}

  /// Decodes an encoded map code into a [HeadlessMatchConfig].
  public static HeadlessMatchConfig parseMapKey(String mapCode, int maxTicks) {
    var params = MapCode.decode(mapCode);

    Terrain terrain = (params.terrainType() == 0) ? Terrain.NATIVE : Terrain.VIKING;
    int metersPerWorld = SIZES[Math.clamp(params.size(), 0, SIZES.length - 1)] * HeightMap.METERS_PER_UNIT_GRID;
    float hills = params.hills() / (float) SLIDER_MAX_VALUE;
    float vegetation = params.vegetation() / (float) SLIDER_MAX_VALUE;
    float supplies = params.supplies() / (float) SLIDER_MAX_VALUE;

    IslandConfig islandConfig = new IslandConfig(terrain, metersPerWorld, hills, vegetation, supplies, params.seed());
    WorldParameters worldParameters = new WorldParameters("mapkey_" + mapCode, Player.INITIAL_UNIT_COUNT, 100);
    UnitInfo defaultUnits = new UnitInfo(true, true, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

    List<HeadlessMatchConfig.PlayerConfig> players = new ArrayList<>();
    Race race0 = params.player0Race() == 0 ? Race.VIKINGS : Race.NATIVES;
    players.add(
        new HeadlessMatchConfig.PlayerConfig(
            params.player0Team(), race0, AIDifficulty.NORMAL, "Player-0", defaultUnits));

    for (int i = 0; i < params.otherSlots().size(); i++) {
      var setting = params.otherSlots().get(i);
      if (setting.difficulty() == 0) {
        continue;
      }
      AIDifficulty slotDifficulty =
          switch (setting.difficulty()) {
            case 1 -> AIDifficulty.EASY;
            case 2 -> AIDifficulty.NORMAL;
            case 3 -> AIDifficulty.HARD;
            default -> AIDifficulty.NORMAL;
          };
      Race race = setting.race() == 0 ? Race.VIKINGS : Race.NATIVES;
      int playerIndex = i + 1;
      players.add(
          new HeadlessMatchConfig.PlayerConfig(
              setting.team(), race, slotDifficulty, "Bot-" + playerIndex, defaultUnits));
    }

    return new HeadlessMatchConfig(
        islandConfig, worldParameters, players, maxTicks, HeadlessMatchConfig.DEFAULT_CHECKSUM_INTERVAL);
  }

  static void main(String[] args) throws Exception {
    int seed = new Random().nextInt();
    int maxTicks = HeadlessMatchConfig.DEFAULT_MAX_TICKS;
    Terrain terrain = Terrain.NATIVE;
    AIDifficulty difficulty = AIDifficulty.NORMAL;
    String mapKey = null;
    Duration stallTimeout = HeadlessMatchConfig.DEFAULT_STALL_TIMEOUT;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--map-key", "--map-code", "-m" -> {
          if (i + 1 < args.length) {
            mapKey = args[++i];
          }
        }
        case "--seed", "-s" -> {
          if (i + 1 < args.length) {
            seed = Integer.parseInt(args[++i]);
          }
        }
        case "--max-ticks", "-t" -> {
          if (i + 1 < args.length) {
            maxTicks = Integer.parseInt(args[++i]);
          }
        }
        case "--stall-timeout" -> {
          if (i + 1 < args.length) {
            long seconds = Long.parseLong(args[++i]);
            stallTimeout = seconds > 0 ? Duration.ofSeconds(seconds) : Duration.ZERO;
          }
        }
        case "--terrain" -> {
          if (i + 1 < args.length) {
            terrain = Terrain.valueOf(args[++i].toUpperCase());
          }
        }
        case "--difficulty", "-d" -> {
          if (i + 1 < args.length) {
            difficulty =
                switch (args[++i].toLowerCase()) {
                  case "easy" -> AIDifficulty.EASY;
                  case "hard" -> AIDifficulty.HARD;
                  default -> AIDifficulty.NORMAL;
                };
          }
        }
        default -> {}
      }
    }

    HeadlessMatchConfig config;
    if (mapKey != null) {
      HeadlessMatchConfig parsed = parseMapKey(mapKey, maxTicks);
      config =
          new HeadlessMatchConfig(
              parsed.islandConfig(),
              parsed.worldParameters(),
              parsed.players(),
              parsed.maxTicks(),
              parsed.checksumIntervalTicks(),
              parsed.timeManager(),
              stallTimeout);
    } else {
      IslandConfig islandConfig =
          new IslandConfig(terrain, 256 * HeightMap.METERS_PER_UNIT_GRID, 0.5f, 0.5f, 0.5f, seed);

      WorldParameters worldParameters = new WorldParameters("headless_seed_" + seed, Player.INITIAL_UNIT_COUNT, 100);

      UnitInfo defaultUnits = new UnitInfo(false, false, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);

      List<HeadlessMatchConfig.PlayerConfig> players =
          List.of(
              new HeadlessMatchConfig.PlayerConfig(0, Race.VIKINGS, difficulty, "Bot-Vikings", defaultUnits),
              new HeadlessMatchConfig.PlayerConfig(1, Race.NATIVES, difficulty, "Bot-Natives", defaultUnits));

      config =
          new HeadlessMatchConfig(
              islandConfig,
              worldParameters,
              players,
              maxTicks,
              HeadlessMatchConfig.DEFAULT_CHECKSUM_INTERVAL,
              new TickTimeManager(),
              stallTimeout);
    }

    HeadlessMatchRunner runner = new HeadlessMatchRunner();
    HeadlessMatchResult result = runner.run(config);

    IO.println("Match concluded:");
    IO.println("  Victory: " + result.victory());
    IO.println("  Winning Team: " + result.winningTeam());
    IO.println("  Final Tick: " + result.finalTick());
    IO.println("  Checksum: " + result.finalChecksum());
    IO.println(String.format(Locale.ROOT, "  Merkle Root: 0x%016x", result.finalMerkleRootHash()));
    IO.println("  Surviving Players: " + result.survivingPlayerIndices());
  }
}
