package com.oddlabs.tt.net;

public strictfp interface GameServerInterface {
	void resetSlotState(int slot, boolean open);
	void setPlayerSlot(int slot,  PlayerSlot.PlayerType type, int race, int team, boolean ready, PlayerSlot.AIType ai_difficulty);
	void startServer();
	void chat(String chat);
}
