package com.oddlabs.tt.core.util;

import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.tt.core.world.WorldGenerator;
import org.jspecify.annotations.NonNull;

/** Factory interface for creating game loading callbacks. */
@FunctionalInterface
public interface LoadCallbackFactory {
    @NonNull
    LoadCallback<?, ?> createCallback(int session_id, @NonNull WorldGenerator generator,
            PlayerSlot @NonNull [] player_slots,
            UnitInfo @NonNull [] unit_infos, short player_slot);
}
