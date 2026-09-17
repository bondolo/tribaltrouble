package com.oddlabs.tt.headless;

import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.player.UnitInfo;

import com.oddlabs.net.TickTimeManager;
import com.oddlabs.net.TimeManager;

import java.util.List;

/**
 * Configuration parameters defining a headless game session.
 *
 * @param islandConfig procedural landscape generation settings
 * @param worldParameters game simulation and unit limits
 * @param players player slot configurations
 * @param maxTicks maximum simulation ticks before match termination
 * @param checksumIntervalTicks frequency of deterministic state checksum validations
 * @param timeManager clock manager driving session networking and pace
 */
public record HeadlessMatchConfig(
                                  IslandConfig islandConfig,
                                  WorldParameters worldParameters,
                                  List<PlayerConfig> players,
                                  int maxTicks,
                                  int checksumIntervalTicks,
                                  TimeManager timeManager) {

    public static final int DEFAULT_MAX_TICKS = 200_000;
    public static final int DEFAULT_CHECKSUM_INTERVAL = 50;

    public HeadlessMatchConfig(
            IslandConfig islandConfig,
            WorldParameters worldParameters,
            List<PlayerConfig> players,
            int maxTicks,
            int checksumIntervalTicks) {
        this(islandConfig, worldParameters, players, maxTicks, checksumIntervalTicks, new TickTimeManager());
    }

    /**
     * Configuration for an individual player slot in a headless match.
     *
     * @param team team identifier (players on the same team share victory)
     * @param race playable faction race
     * @param aiDifficulty AI difficulty tier
     * @param name display name for the player
     * @param unitInfo starting army and structure configuration
     */
    public record PlayerConfig(
                               int team,
                               Race race,
                               int aiDifficulty,
                               String name,
                               UnitInfo unitInfo) {
    }

    public HeadlessMatchConfig {
        players = List.copyOf(players);
    }
}
