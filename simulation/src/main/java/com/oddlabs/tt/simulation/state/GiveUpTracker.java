package com.oddlabs.tt.simulation.state;

import java.util.EnumSet;

/// One-shot per-phase progress latch preventing states from looping indefinitely without making progress.
final class GiveUpTracker<E extends Enum<E>> {
  private final EnumSet<E> attempted;

  GiveUpTracker(Class<E> phaseType) {
    this.attempted = EnumSet.noneOf(phaseType);
  }

  void resetAll() {
    attempted.clear();
  }

  void reset(E state) {
    attempted.remove(state);
  }

  /// Marks `state` as attempted and returns `false` on the first call, or `true` if already attempted.
  boolean shouldGiveUp(E state) {
    return !attempted.add(state);
  }

  /// Single-phase one-shot progress latch avoiding set allocation for states with one phase.
  static final class Single {
    private boolean attempted;

    void reset() {
      attempted = false;
    }

    /// Marks the phase as attempted and returns `false` on the first call, or `true` if already attempted.
    boolean shouldGiveUp() {
      if (!attempted) {
        attempted = true;
        return false;
      }
      return true;
    }
  }
}
