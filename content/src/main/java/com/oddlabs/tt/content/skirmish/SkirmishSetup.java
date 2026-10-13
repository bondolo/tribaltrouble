package com.oddlabs.tt.content.skirmish;

import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.player.PlayerSlot;

import java.util.List;

/**
 * Defines a skirmish match setup composing WorldParameters, IslandConfig, and bot slot configurations.
 */
public record SkirmishSetup(
                            WorldParameters worldParameters,
                            IslandConfig islandConfig,
                            List<SkirmishPlayerSlot> playerSlots
) {
    public SkirmishSetup {
        playerSlots = List.copyOf(playerSlots);
    }

    /**
     * Validates that the skirmish setup has at least two opposing teams.
     */
    public boolean hasEnemyTeams() {
        if (playerSlots.isEmpty()) {
            return false;
        }
        int firstTeam = playerSlots.getFirst().team();
        for (int i = 1; i < playerSlots.size(); i++) {
            if (playerSlots.get(i).team() != firstTeam) {
                return true;
            }
        }
        return false;
    }

    /**
     * Configures player and AI bot slots on the game network server interface and starts the server.
     */
    public void startSkirmishServer(GameNetwork gameNetwork) {
        var serverInterface = gameNetwork.getClient().getServerInterface();
        for (SkirmishPlayerSlot slot : playerSlots) {
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
