package com.oddlabs.tt.simulation;

/// Listener notified at each simulation tick.
@FunctionalInterface
public interface TickListener {
  void onTick();
}
