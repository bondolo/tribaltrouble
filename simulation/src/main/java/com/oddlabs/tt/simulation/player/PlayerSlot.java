package com.oddlabs.tt.simulation.player;

import com.oddlabs.matchmaking.TunnelAddress;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/** Slot configuration holding player type, team, race, and readiness. */
public final class PlayerSlot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public static final int AI_NONE = AIDifficulty.NONE.getValue();
    public static final int AI_EASY = AIDifficulty.EASY.getValue();
    public static final int AI_NORMAL = AIDifficulty.NORMAL.getValue();
    public static final int AI_HARD = AIDifficulty.HARD.getValue();
    public static final int AI_TOWER_TUTORIAL = AIDifficulty.TOWER_TUTORIAL.getValue();
    public static final int AI_CHIEFTAIN_TUTORIAL = AIDifficulty.CHIEFTAIN_TUTORIAL.getValue();
    public static final int AI_BATTLE_TUTORIAL = AIDifficulty.BATTLE_TUTORIAL.getValue();
    public static final int AI_PASSIVE_CAMPAIGN = AIDifficulty.PASSIVE_CAMPAIGN.getValue();
    public static final int AI_NEUTRAL_CAMPAIGN = AIDifficulty.NEUTRAL_CAMPAIGN.getValue();

    public static final int OPEN = SlotType.OPEN.getValue();
    public static final int CLOSED = SlotType.CLOSED.getValue();
    public static final int HUMAN = SlotType.HUMAN.getValue();
    public static final int AI = SlotType.AI.getValue();

    private final int slot;

    private SlotType type = SlotType.OPEN;
    private int rating;
    private boolean ready;
    private @Nullable PlayerInfo player_info;
    private @Nullable TunnelAddress address;
    private AIDifficulty ai_difficulty = AIDifficulty.NONE;
    private transient @Nullable Boolean network_peer = null;

    public PlayerSlot(int slot) {
        this.slot = slot;
    }

    public static boolean isValidType(int type) {
        return type == HUMAN || type == AI/* || type == OPEN || type == CLOSED*/;
    }

    public static boolean isValidType(SlotType type) {
        return type == SlotType.HUMAN || type == SlotType.AI;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setType(SlotType type) {
        this.type = type;
    }

    public void setType(int type) {
        this.type = SlotType.fromValue(type);
    }

    public void setAIDifficulty(AIDifficulty ai_difficulty) {
        this.ai_difficulty = ai_difficulty;
    }

    public void setAIDifficulty(int ai_difficulty) {
        this.ai_difficulty = AIDifficulty.fromValue(ai_difficulty);
    }

    public void setAddress(@Nullable TunnelAddress address) {
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

    public @Nullable PlayerInfo getInfo() {
        return player_info;
    }

    public boolean isReady() {
        return ready;
    }

    public @Nullable TunnelAddress getAddress() {
        return address;
    }

    public AIDifficulty getAIDifficulty() {
        return ai_difficulty;
    }

    public SlotType getType() {
        return type;
    }

    public int getRating() {
        return rating;
    }

    public boolean isNetworkPeer() {
        if (network_peer != null) {
            return network_peer;
        }
        return type == SlotType.HUMAN;
    }

    public void setNetworkPeer(boolean network_peer) {
        this.network_peer = network_peer;
    }

    @Override
    public String toString() {
        return String.valueOf(player_info);
    }
}
