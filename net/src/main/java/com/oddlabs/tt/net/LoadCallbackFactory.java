package com.oddlabs.tt.net;

import com.oddlabs.tt.base.util.LoadCallback;

import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;

/** Factory interface for creating game loading callbacks. */
@FunctionalInterface
public interface LoadCallbackFactory<C, R> {
    LoadCallback<C, R> createCallback(int session_id, WorldGenerator<?> generator,
            PlayerSlot[] player_slots,
            UnitInfo[] unit_infos, short player_slot);
}
