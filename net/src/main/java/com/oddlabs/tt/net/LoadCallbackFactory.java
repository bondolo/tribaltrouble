package com.oddlabs.tt.net;

import com.oddlabs.tt.base.util.LoadCallback;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import java.util.List;

/// Factory interface for creating game loading callbacks.
@FunctionalInterface
public interface LoadCallbackFactory {
  LoadCallback<?, ?> createCallback(
      int session_id,
      WorldGenerator<?> generator,
      List<PlayerSlot> player_slots,
      List<UnitInfo> unit_infos,
      short player_slot);
}
