package com.oddlabs.tt.net;

import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.player.AIDifficulty;
import com.oddlabs.tt.simulation.player.SlotType;

/// Remote network interface for server commands including slot allocation and match startup.
public interface GameServerInterface {
  void resetSlotState(int slot, boolean open);

  void setPlayerSlot(int slot, SlotType type, Race race, int team, boolean ready, AIDifficulty ai_difficulty);

  void startServer();

  void chat(String chat);
}
