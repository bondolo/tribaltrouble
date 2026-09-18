package com.oddlabs.tt.headless;

import com.oddlabs.tt.net.HeadlessMultiplayerHarness;
import com.oddlabs.tt.net.HeadlessSimulationInstance;
import com.oddlabs.tt.procedural.landscape.GeneratedLandscapeData;
import com.oddlabs.tt.procedural.landscape.IslandGenerator;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Orchestrator that provisions procedural islands, initializes peer instances, and runs simulation matches to victory.
 */
public final class HeadlessMatchRunner {
    private static final Logger logger = Logger.getLogger(HeadlessMatchRunner.class.getName());

    /**
     * Executes a headless match until only one team remains alive, or until the maximum tick threshold is reached.
     *
     * @param config configuration for the match
     * @return summary result containing the winning team and termination metrics
     * @throws TimeoutException if network synchronization fails to complete
     * @throws InterruptedException if interrupted while executing
     */
    public HeadlessMatchResult run(HeadlessMatchConfig config) throws TimeoutException, InterruptedException {
        int numPlayers = config.players().size();
        PlayerSlot[] playerSlots = new PlayerSlot[numPlayers];
        UnitInfo[] unitInfos = new UnitInfo[numPlayers];

        for (int i = 0; i < numPlayers; i++) {
            HeadlessMatchConfig.PlayerConfig playerConfig = config.players().get(i);
            PlayerSlot slot = new PlayerSlot(i);
            slot.setType(PlayerSlot.AI);
            slot.setAIDifficulty(playerConfig.aiDifficulty());
            slot.setReady(true);
            slot.setInfo(new PlayerInfo(playerConfig.team(), playerConfig.race(), playerConfig.name()));
            playerSlots[i] = slot;
            unitInfos[i] = playerConfig.unitInfo();
        }

        IslandGenerator generator = new IslandGenerator(config.islandConfig());
        GeneratedLandscapeData landscapeData = generator.generate(
                numPlayers,
                config.worldParameters().initialUnitCount(),
                0.0f
        );

        try (HeadlessMultiplayerHarness harness = HeadlessMultiplayerHarness.create(
                landscapeData,
                config.worldParameters(),
                playerSlots,
                unitInfos,
                config.timeManager(),
                true)) {

            harness.awaitSynchronized(Duration.ofSeconds(10));
            harness.verifyChecksums();

            int initialTick = harness.getInstances().getFirst().getTick();
            int maxTicks = config.maxTicks();
            int checksumInterval = config.checksumIntervalTicks();
            final int progressIntervalTicks = 5_000;
            long lastProgressTimeNanos = System.nanoTime();
            int lastProgressTick = initialTick;
            long lastTickAdvanceNanos = System.nanoTime();
            int lastObservedTick = initialTick;
            Duration stallTimeout = config.stallTimeout();
            long stallTimeoutNanos = stallTimeout != null ? stallTimeout.toNanos() : 0L;

            while (true) {
                harness.stepConcurrent();
                if (!harness.areTicksAligned()) {
                    if (stallTimeoutNanos > 0L && System.nanoTime() - lastTickAdvanceNanos > stallTimeoutNanos) {
                        throw new TimeoutException("Simulation stalled: instance ticks failed to align within "
                                + stallTimeout);
                    }
                    continue;
                }
                int currentTick = harness.getInstances().getFirst().getTick();
                int elapsedTicks = currentTick - initialTick;

                if (currentTick != lastObservedTick) {
                    lastObservedTick = currentTick;
                    lastTickAdvanceNanos = System.nanoTime();
                } else if (stallTimeoutNanos > 0L && System.nanoTime() - lastTickAdvanceNanos > stallTimeoutNanos) {
                    throw new TimeoutException("Simulation stalled: tick " + currentTick + " has not advanced for "
                            + stallTimeout);
                }

                if (checksumInterval > 0 && currentTick % checksumInterval == 0) {
                    harness.verifyChecksums();
                }

                if (elapsedTicks > 0 && elapsedTicks % progressIntervalTicks == 0) {
                    long nowNanos = System.nanoTime();
                    double seconds = (nowNanos - lastProgressTimeNanos) / 1_000_000_000.0;
                    double ticksPerSec = seconds > 0.0 ? (currentTick - lastProgressTick) / seconds : 0.0;
                    lastProgressTimeNanos = nowNanos;
                    lastProgressTick = currentTick;

                    StringBuilder progress = new StringBuilder();
                    progress.append(String.format("Tick %d (+%d/%d, %.1f ticks/s):", currentTick, elapsedTicks,
                            maxTicks, ticksPerSec));
                    for (HeadlessSimulationInstance instance : harness.getInstances()) {
                        Player player = instance.getLocalPlayer();
                        int units = player.getUnitCountContainer().getNumSupplies();
                        int buildings = player.getBuildingCountContainer().getNumSupplies();
                        progress.append(String.format(" [%s(T%d): %d units, %d bldgs, alive=%b]",
                                player.getPlayerInfo().getName(),
                                player.getPlayerInfo().getTeam(),
                                units,
                                buildings,
                                instance.isAlive()));
                    }
                    IO.println(progress);
                }

                Set<Integer> aliveTeams = new HashSet<>();
                List<Integer> survivingPlayers = new ArrayList<>();
                for (HeadlessSimulationInstance instance : harness.getInstances()) {
                    if (instance.isAlive()) {
                        survivingPlayers.add(instance.getPlayerIndex());
                        int team = instance.getLocalPlayer().getPlayerInfo().getTeam();
                        if (team != PlayerInfo.TEAM_NEUTRAL) {
                            aliveTeams.add(team);
                        }
                    }
                }

                if (aliveTeams.size() == 1) {
                    harness.verifyChecksums();
                    int winningTeam = aliveTeams.iterator().next();
                    logger.info(() -> "Victory achieved by team " + winningTeam + " at tick " + currentTick);
                    return new HeadlessMatchResult(
                            winningTeam,
                            true,
                            currentTick,
                            harness.getInstances().getFirst().getChecksum(),
                            survivingPlayers
                    );
                }

                if (aliveTeams.isEmpty()) {
                    harness.verifyChecksums();
                    logger.info(() -> "All teams eliminated at tick " + currentTick);
                    return new HeadlessMatchResult(
                            -1,
                            false,
                            currentTick,
                            harness.getInstances().getFirst().getChecksum(),
                            survivingPlayers
                    );
                }

                if (elapsedTicks >= maxTicks) {
                    harness.verifyChecksums();
                    logger.info(() -> "Match reached maximum tick limit " + maxTicks + " (elapsed ticks: "
                            + elapsedTicks + ")");
                    return new HeadlessMatchResult(
                            -1,
                            false,
                            currentTick,
                            harness.getInstances().getFirst().getChecksum(),
                            survivingPlayers
                    );
                }
            }
        }
    }
}
