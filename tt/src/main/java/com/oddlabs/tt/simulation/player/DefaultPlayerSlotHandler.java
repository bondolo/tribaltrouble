package com.oddlabs.tt.simulation.player;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/** Default implementation of PlayerSlotHandler for PlayerSlot objects. */
public final class DefaultPlayerSlotHandler implements PlayerSlotHandler {
    @Override
    public Serializable createSlot(int slot) {
        PlayerSlot playerSlot = new PlayerSlot(slot);
        playerSlot.setReady(slot != 0);
        return playerSlot;
    }

    @Override
    public boolean isValidType(int type) {
        return PlayerSlot.isValidType(type);
    }

    @Override
    public int getHumanType() {
        return PlayerSlot.HUMAN;
    }

    @Override
    public int getAiType() {
        return PlayerSlot.AI;
    }

    @Override
    public int getOpenType() {
        return PlayerSlot.OPEN;
    }

    @Override
    public int getClosedType() {
        return PlayerSlot.CLOSED;
    }

    @Override
    public int getAiNone() {
        return PlayerSlot.AI_NONE;
    }

    @Override
    public int getSlotIndex(Serializable slot) {
        return ((PlayerSlot) slot).getSlot();
    }

    @Override
    public int getType(Serializable slot) {
        return ((PlayerSlot) slot).getType();
    }

    @Override
    public int getAIDifficulty(Serializable slot) {
        return ((PlayerSlot) slot).getAIDifficulty();
    }

    @Override
    public boolean isReady(Serializable slot) {
        return ((PlayerSlot) slot).isReady();
    }

    @Override
    public @Nullable Serializable getInfo(Serializable slot) {
        return ((PlayerSlot) slot).getInfo();
    }

    @Override
    public String getPlayerName(Serializable slot) {
        PlayerSlot s = (PlayerSlot) slot;
        return s.getInfo() != null ? s.getInfo().toString() : "Player " + s.getSlot();
    }

    @Override
    public void resetSlot(Serializable slot, boolean open) {
        PlayerSlot s = (PlayerSlot) slot;
        s.setType(open ? PlayerSlot.OPEN : PlayerSlot.CLOSED);
        s.setInfo(null);
        s.setAddress(null);
        s.setReady(true);
        s.setAIDifficulty(PlayerSlot.AI_NONE);
    }

    @Override
    public void updateSlot(Serializable slot, int type, int ai_difficulty, @Nullable Serializable info, boolean ready) {
        PlayerSlot s = (PlayerSlot) slot;
        s.setType(type);
        s.setAIDifficulty(ai_difficulty);
        s.setInfo((PlayerInfo) info);
        s.setReady(type != PlayerSlot.HUMAN || ready);
    }
}
