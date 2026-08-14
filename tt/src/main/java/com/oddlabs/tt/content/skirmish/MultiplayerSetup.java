package com.oddlabs.tt.content.skirmish;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import org.jspecify.annotations.NonNull;

/**
 * Defines a complete multiplayer match setup composing WorldParameters, Game, IslandConfig, and player info.
 */
public final class MultiplayerSetup {
    private final @NonNull WorldParameters worldParameters;
    private final @NonNull IslandConfig islandConfig;
    private final @NonNull Game game;
    private final @NonNull Race player0_race;
    private final int player0_team;

    public MultiplayerSetup(@NonNull WorldParameters worldParameters, @NonNull Game game,
            @NonNull IslandConfig islandConfig, @NonNull Race player0_race, int player0_team) {
        this.worldParameters = worldParameters;
        this.game = game;
        this.islandConfig = islandConfig;
        this.player0_race = player0_race;
        this.player0_team = player0_team;
    }

    public @NonNull WorldParameters getWorldParameters() {
        return worldParameters;
    }

    public @NonNull Game getGame() {
        return game;
    }

    public @NonNull IslandConfig getIslandConfig() {
        return islandConfig;
    }

    public @NonNull Race getPlayer0Race() {
        return player0_race;
    }

    public int getPlayer0Team() {
        return player0_team;
    }
}
