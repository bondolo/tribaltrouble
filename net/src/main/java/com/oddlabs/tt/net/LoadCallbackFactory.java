package com.oddlabs.tt.net;

import java.util.List;

import com.oddlabs.tt.base.util.LoadCallback;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;

/** Factory interface for creating game loading callbacks. */
@FunctionalInterface
public interface LoadCallbackFactory<C, R> {
    LoadCallback<C, R> createCallback(int session_id, WorldGenerator<?> generator,
            List<PlayerSlot> player_slots,
            List<UnitInfo> unit_infos, short player_slot);
}
