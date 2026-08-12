package com.oddlabs.tt.simulation.player;

import com.oddlabs.matchmaking.TunnelAddress;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/** Slot configuration holding player type, team, race, and readiness. */
public final class PlayerSlot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public static final int AI_NONE = 0;
    public static final int AI_EASY = 1;
    public static final int AI_NORMAL = 2;
    public static final int AI_HARD = 3;
    public static final int AI_TOWER_TUTORIAL = 4;
    public static final int AI_CHIEFTAIN_TUTORIAL = 5;
    public static final int AI_BATTLE_TUTORIAL = 6;
    public static final int AI_PASSIVE_CAMPAIGN = 7;
    public static final int AI_NEUTRAL_CAMPAIGN = 8;

    public static final int OPEN = 1;
    public static final int CLOSED = 2;
    public static final int HUMAN = 3;
    public static final int AI = 4;

    private final int slot;

    private int type = OPEN;
    private int rating;
    private boolean ready;
    private @Nullable PlayerInfo player_info;
    private TunnelAddress address;
    private int ai_difficulty = AI_NONE;

    public PlayerSlot(int slot) {
        this.slot = slot;
    }

    public static boolean isValidType(int type) {
        return type == HUMAN || type == AI/* || type == OPEN || type == CLOSED*/;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setAIDifficulty(int ai_difficulty) {
        this.ai_difficulty = ai_difficulty;
    }

    public void setAddress(TunnelAddress address) {
        this.address = address;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public int getSlot() {
        return slot;
    }

    public void setInfo(@Nullable PlayerInfo player_info) {
        this.player_info = player_info;
    }

    public Serializable getInfo() {
        return player_info;
    }

    public boolean isReady() {
        return ready;
    }

    public TunnelAddress getAddress() {
        return address;
    }

    public int getAIDifficulty() {
        return ai_difficulty;
    }

    public int getType() {
        return type;
    }

    public int getRating() {
        return rating;
    }

    @Override
    public String toString() {
        return String.valueOf(player_info);
    }
}
