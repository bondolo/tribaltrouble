package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.TunnelAddress;
import com.oddlabs.tt.player.PlayerInfo;
import java.io.Serializable;

public final strictfp class PlayerSlot implements Serializable {
	private final static long serialVersionUID = 1;

    public enum AIType {
        AI_NONE, AI_EASY, AI_NORMAL, AI_HARD,
        AI_TOWER_TUTORIAL, AI_CHIEFTAIN_TUTORIAL, AI_BATTLE_TUTORIAL,
        AI_PASSIVE_CAMPAIGN, AI_NEUTRAL_CAMPAIGN
    }

    public enum PlayerType {
        OPEN, CLOSED, HUMAN,AI
    }

	private final int slot;

	private PlayerType type = PlayerType.OPEN;
	private int rating;
	private boolean ready;
	private PlayerInfo player_info;
	private TunnelAddress address;
	private AIType ai_difficulty = AIType.AI_NONE;

	PlayerSlot(int slot) {
		this.slot = slot;
	}

	static boolean isValidType(PlayerType type) {
		return type == PlayerType.HUMAN || type == PlayerType.AI/* || type == PlayerType.OPEN || type == PlayerType.CLOSED*/;
	}

	void setRating(int rating) {
		this.rating = rating;
	}

	void setType(PlayerType type) {
		this.type = type;
	}

	void setAIDifficulty(AIType ai_difficulty) {
		this.ai_difficulty = ai_difficulty;
	}

	void setAddress(TunnelAddress address) {
		this.address = address;
	}

	void setReady(boolean ready) {
		this.ready = ready;
	}

	int getSlot() {
		return slot;
	}

	void setInfo(PlayerInfo player_info) {
		this.player_info = player_info;
	}

	public PlayerInfo getInfo() {
		return player_info;
	}

	public boolean isReady() {
		return ready;
	}

	public TunnelAddress getAddress() {
		return address;
	}

	public AIType getAIDifficulty() {
		return ai_difficulty;
	}

	public PlayerType getType() {
		return type;
	}

	public int getRating() {
		return rating;
	}
}
