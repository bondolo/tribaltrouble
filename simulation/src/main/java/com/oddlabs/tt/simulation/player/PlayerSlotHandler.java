package com.oddlabs.tt.simulation.player;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * Interface for manipulating player slot configuration state.
 */
public interface PlayerSlotHandler {
    Serializable createSlot(int slot);

    boolean isValidType(int type);

    int getHumanType();

    int getAiType();

    int getOpenType();

    int getClosedType();

    int getAiNone();

    int getSlotIndex(Serializable slot);

    int getType(Serializable slot);

    int getAIDifficulty(Serializable slot);

    boolean isReady(Serializable slot);

    @Nullable
    Serializable getInfo(Serializable slot);

    String getPlayerName(Serializable slot);

    void resetSlot(Serializable slot, boolean open);

    void updateSlot(Serializable slot, int type, int ai_difficulty, @Nullable Serializable info, boolean ready);
}
