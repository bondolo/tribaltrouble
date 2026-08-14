package com.oddlabs.tt.content.skirmish;

import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Defines a skirmish match setup composing WorldParameters, IslandConfig, and bot slot configurations.
 */
public final class SkirmishSetup {
    private final @NonNull WorldParameters worldParameters;
    private final @NonNull IslandConfig islandConfig;
    private final @NonNull List<SkirmishPlayerSlot> player_slots;

    public SkirmishSetup(@NonNull WorldParameters worldParameters, @NonNull IslandConfig islandConfig,
            @NonNull List<SkirmishPlayerSlot> player_slots) {
        this.worldParameters = worldParameters;
        this.islandConfig = islandConfig;
        this.player_slots = List.copyOf(player_slots);
    }

    public @NonNull WorldParameters getWorldParameters() {
        return worldParameters;
    }

    public @NonNull IslandConfig getIslandConfig() {
        return islandConfig;
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
