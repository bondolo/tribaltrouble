package com.oddlabs.tt.content.skirmish;

import com.oddlabs.tt.core.net.GameNetwork;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Terrain;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Defines a skirmish match setup extending WorldParameters with terrain and bot slot configurations.
 */
public class SkirmishSetup extends WorldParameters {
    private final @NonNull Terrain terrain;
    private final int map_size;
    private final float hills;
    private final float vegetation;
    private final float supplies;
    private final long seed;
    private final @NonNull List<SkirmishPlayerSlot> player_slots;

    public SkirmishSetup(int initial_game_speed, @NonNull String map_code, int initial_unit_count,
            int max_unit_count, @NonNull Terrain terrain, int map_size, float hills, float vegetation,
            float supplies, long seed, @NonNull List<SkirmishPlayerSlot> player_slots) {
        super(initial_game_speed, map_code, initial_unit_count, max_unit_count);
        this.terrain = terrain;
        this.map_size = map_size;
        this.hills = hills;
        this.vegetation = vegetation;
        this.supplies = supplies;
        this.seed = seed;
        this.player_slots = List.copyOf(player_slots);
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

    public @NonNull List<SkirmishPlayerSlot> getPlayerSlots() {
        return player_slots;
    }

    /**
     * Validates that the skirmish setup has at least two opposing teams.
     */
    public boolean hasEnemyTeams() {
        if (player_slots.isEmpty()) {
            return false;
        }
        int player0Team = player_slots.getFirst().team();
        for (int i = 1; i < player_slots.size(); i++) {
            if (player_slots.get(i).team() != player0Team) {
                return true;
            }
        }
        return false;
    }

    /**
     * Configures player and AI bot slots on the game network server interface and starts the server.
     */
    public void startSkirmishServer(@NonNull GameNetwork gameNetwork) {
        var serverInterface = gameNetwork.getClient().getServerInterface();
        for (SkirmishPlayerSlot slot : player_slots) {
            if (slot.isHuman()) {
                serverInterface.setPlayerSlot(slot.slotIndex(), PlayerSlot.HUMAN, slot.race().getValue(),
                        slot.team(), true, PlayerSlot.AI_NONE);
            } else {
                serverInterface.setPlayerSlot(slot.slotIndex(), PlayerSlot.AI, slot.race().getValue(),
                        slot.team(), true, slot.aiDifficulty());
            }
        }
        serverInterface.startServer();
    }
}
