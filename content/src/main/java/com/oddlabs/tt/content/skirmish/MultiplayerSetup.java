package com.oddlabs.tt.content.skirmish;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;

/**
 * Defines a complete multiplayer match setup composing WorldParameters, Game, IslandConfig, and player info.
 */
public final class MultiplayerSetup {
    private final WorldParameters worldParameters;
    private final IslandConfig islandConfig;
    private final Game game;
    private final Race player0_race;
    private final int player0_team;

    public MultiplayerSetup(WorldParameters worldParameters, Game game,
            IslandConfig islandConfig, Race player0_race, int player0_team) {
        this.worldParameters = worldParameters;
        this.game = game;
        this.islandConfig = islandConfig;
        this.player0_race = player0_race;
        this.player0_team = player0_team;
    }

    public WorldParameters getWorldParameters() {
        return worldParameters;
    }

    public Game getGame() {
        return game;
    }

    public IslandConfig getIslandConfig() {
        return islandConfig;
    }

    public Race getPlayer0Race() {
        return player0_race;
    }

    public int getPlayer0Team() {
        return player0_team;
    }
}
