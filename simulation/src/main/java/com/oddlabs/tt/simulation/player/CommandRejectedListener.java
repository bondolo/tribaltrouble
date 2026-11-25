package com.oddlabs.tt.simulation.player;

/// Listener notified when a simulation player command cannot be applied due to gameplay conditions (such as a missing
/// or unowned entity).
@FunctionalInterface
public interface CommandRejectedListener {

  /// Called when a player command is rejected and dropped.
  ///
  /// @param entityId primary entity ID involved, or -1 if none
  void onCommandRejected(String action, int entityId, String reason);
}
