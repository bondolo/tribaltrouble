package com.oddlabs.tt.content.skirmish;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Terrain;
import org.jspecify.annotations.NonNull;

/**
 * Defines a complete multiplayer match setup extending WorldParameters.
 */
public class MultiplayerSetup extends WorldParameters {
    private final @NonNull Game game;
    private final @NonNull Terrain terrain;
    private final int map_size;
    private final float hills;
    private final float vegetation;
    private final float supplies;
    private final long seed;
    private final @NonNull Race player0_race;
    private final int player0_team;

    public MultiplayerSetup(int initial_game_speed, @NonNull String map_code, int initial_unit_count,
            int max_unit_count, @NonNull Game game, @NonNull Terrain terrain, int map_size, float hills,
            float vegetation, float supplies, long seed, @NonNull Race player0_race, int player0_team) {
        super(initial_game_speed, map_code, initial_unit_count, max_unit_count);
        this.game = game;
        this.terrain = terrain;
        this.map_size = map_size;
        this.hills = hills;
        this.vegetation = vegetation;
        this.supplies = supplies;
        this.seed = seed;
        this.player0_race = player0_race;
        this.player0_team = player0_team;
    }

    public @NonNull Game getGame() {
        return game;
    }

    public @NonNull Terrain getTerrain() {
        return terrain;
    }

    public int getMapSize() {
        return map_size;
    }

    public float getHills() {
        return hills;
    }

    public float getVegetation() {
        return vegetation;
    }

    public float getSupplies() {
        return supplies;
    }

    public long getSeed() {
        return seed;
    }

    public @NonNull Race getPlayer0Race() {
        return player0_race;
    }

    public int getPlayer0Team() {
        return player0_team;
    }
}
