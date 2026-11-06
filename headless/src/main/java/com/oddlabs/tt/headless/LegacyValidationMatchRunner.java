package com.oddlabs.tt.headless;

import com.oddlabs.tt.net.HeadlessSimulationInstance;
import com.oddlabs.tt.procedural.landscape.IslandGenerator;
import com.oddlabs.tt.simulation.player.CommandRejectedListener;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.LandscapeGeometry;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.player.AI;
import com.oddlabs.tt.simulation.player.AdvancedAI;
import com.oddlabs.tt.simulation.player.PassiveAI;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerInitializer;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs a single-world, deterministic validation match matching legacy game execution.
 */
@NullMarked
public final class LegacyValidationMatchRunner {
    private static final Logger logger = Logger.getLogger(LegacyValidationMatchRunner.class.getSimpleName());

    public HeadlessMatchResult run(HeadlessMatchConfig config) {
        IslandGenerator generator = new IslandGenerator(config.islandConfig());
        Landscape landscape = generator.generate(
                config.players().size(),
                config.worldParameters().initialUnitCount(),
                0f
        );

        List<PlayerInfo> playerInfos = config.players().stream()
                .map(p -> new PlayerInfo(p.team(), p.race(), p.name()))
                .toList();

        World world = World.newWorld(
                new RaceData(),
                new LandscapeGeometry(),
                new NotificationListener() {
                },
                config.worldParameters(),
                landscape,
                playerInfos,
                HeadlessSimulationInstance.DEFAULT_PLAYER_COLORS
        );

        for (int i = 0; i < config.players().size(); i++) {
            var pConfig = config.players().get(i);
            Player player = world.getPlayers().get(i);
            PlayerInitializer.initializePlayerArmy(
                    player,
                    landscape.startingLocations().get(i),
                    pConfig.unitInfo(),
                    config.worldParameters().initialGameSpeed(),
                    null
            );

            AI ai = switch (pConfig.aiDifficulty()) {
                case PlayerSlot.AI_NORMAL -> new AdvancedAI(player, player, Difficulty.NORMAL);
                case PlayerSlot.AI_HARD -> new AdvancedAI(player, player, Difficulty.HARD);
                case PlayerSlot.AI_EASY -> new AdvancedAI(player, player, Difficulty.EASY);
                case PlayerSlot.AI_BATTLE_TUTORIAL, PlayerSlot.AI_PASSIVE_CAMPAIGN ->
                    new PassiveAI(player, player, true);
                case PlayerSlot.AI_NEUTRAL_CAMPAIGN ->
                    new PassiveAI(player, player, false);
                default -> throw new IllegalArgumentException("Unsupported difficulty: " + pConfig.aiDifficulty());
            };
            player.setAI(ai);
        }

        for (Player p : world.getPlayers()) {
            p.setCommandRejectedListener((action, entityId, reason) ->
                    logger.log(Level.FINE, "Command rejected on player {0} for {1} (entity {2}): {3}",
                            new Object[]{p.getPlayerInfo().name(), action, entityId, reason}));
        }

        int maxTicks = config.maxTicks();
        List<Player> players = world.getPlayers();
        int numPlayers = players.size();

        String dumpTicks = System.getProperty("dumpTicks");
        String dumpIntervalStr = System.getProperty("dumpInterval");
        int dumpInterval = dumpIntervalStr != null ? Integer.parseInt(dumpIntervalStr) : 0;

        for (int currentTick = 1; currentTick <= maxTicks; currentTick++) {
            world.tick();
            if (dumpInterval > 0 && currentTick % dumpInterval == 0) {
                System.out.printf("MODERN TICK %d: checksum=%d%n", currentTick, world.getChecksum());
            }
            if (dumpTicks != null && matchesFilter(currentTick, dumpTicks)) {
                System.out.printf("MODERN TICK %d: checksum=%d%n", currentTick, world.getChecksum());
                int idx = 0;
                for (Player p : world.getPlayers()) {
                    for (com.oddlabs.tt.simulation.model.Selectable<?> s : p.getUnits().getSet()) {
                        String tgtStr = "";
                        if (s.getCurrentController() instanceof com.oddlabs.tt.simulation.behaviour.WalkController wc) {
                            com.oddlabs.tt.simulation.model.Target tgt = wc.getTarget();
                            tgtStr = " tgt=(" + tgt.getPositionX() + "," + tgt.getPositionY() + ") grid=(" + tgt
                                    .getGridX() + "," + tgt.getGridY() + ")";
                        }
                        System.out.printf("MODERN UNIT t=%d i=%d p=%s pos=(%.6f,%.6f) beh=%s ctrl=%s%s%n",
                                currentTick, idx++, p.getPlayerInfo().name(), s.getPositionX(), s.getPositionY(),
                                s.getCurrentBehaviour() != null ? s.getCurrentBehaviour().getClass().getSimpleName()
                                        : "null",
                                s.getCurrentController() != null ? s.getCurrentController().getClass().getSimpleName()
                                        : "null",
                                tgtStr);
                    }
                }
            }

            int firstAliveTeam = 0;
            boolean hasFirstTeam = false;
            boolean multipleTeamsAlive = false;
            boolean anyPlayerAlive = false;

            for (int i = 0; i < numPlayers; i++) {
                Player player = players.get(i);
                if (player.isAlive()) {
                    anyPlayerAlive = true;
                    int team = player.getPlayerInfo().team();
                    if (!hasFirstTeam) {
                        firstAliveTeam = team;
                        hasFirstTeam = true;
                    } else if (firstAliveTeam != team) {
                        multipleTeamsAlive = true;
                        break;
                    }
                }
            }

            int tick = currentTick;
            if (!multipleTeamsAlive && hasFirstTeam) {
                int winningTeam = firstAliveTeam;
                logger.info(() -> "Legacy validation victory achieved by team " + winningTeam + " at tick " + tick);
                List<Integer> survivingPlayers = new ArrayList<>();
                for (int i = 0; i < numPlayers; i++) {
                    if (players.get(i).isAlive()) {
                        survivingPlayers.add(i);
                    }
                }
                return new HeadlessMatchResult(
                        winningTeam,
                        true,
                        world.getTick(),
                        world.getChecksum(),
                        survivingPlayers
                );
            }

            if (!anyPlayerAlive) {
                logger.info(() -> "All teams eliminated at tick " + tick);
                return new HeadlessMatchResult(
                        -1,
                        false,
                        world.getTick(),
                        world.getChecksum(),
                        List.of()
                );
            }
        }

        logger.info(() -> "Match reached maximum tick limit " + maxTicks);
        List<Integer> survivingPlayers = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            if (players.get(i).isAlive()) {
                survivingPlayers.add(i);
            }
        }
        return new HeadlessMatchResult(
                -1,
                false,
                world.getTick(),
                world.getChecksum(),
                survivingPlayers
        );
    }

    static boolean matchesFilter(int value, @Nullable String filterSpec) {
        if (filterSpec == null || filterSpec.isBlank()) {
            return true;
        }
        for (String token : filterSpec.split(",")) {
            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.contains("..")) {
                String[] bounds = token.split("\\.\\.", -1);
                int min = bounds[0].isBlank() ? Integer.MIN_VALUE : Integer.parseInt(bounds[0].trim());
                int max = bounds.length > 1 && !bounds[1].isBlank() ? Integer.parseInt(bounds[1].trim())
                        : Integer.MAX_VALUE;
                if (value >= min && value <= max) {
                    return true;
                }
            } else if (token.contains("-")) {
                String[] bounds = token.split("-", -1);
                int min = bounds[0].isBlank() ? Integer.MIN_VALUE : Integer.parseInt(bounds[0].trim());
                int max = bounds.length > 1 && !bounds[1].isBlank() ? Integer.parseInt(bounds[1].trim())
                        : Integer.MAX_VALUE;
                if (value >= min && value <= max) {
                    return true;
                }
            } else {
                if (value == Integer.parseInt(token)) {
                    return true;
                }
            }
        }
        return false;
    }
}
