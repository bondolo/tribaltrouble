package com.oddlabs.net;

/// Deterministic, tick-based monotonic clock for simulation and headless network lockstep execution.
public final class TickTimeManager implements TimeManager {

  /// Standard simulation milliseconds per tick at 50Hz (20ms).
  public static final long DEFAULT_MILLIS_PER_TICK = 20L;

  private final long millisPerTick;
  private long currentMillis;

  public TickTimeManager() {
    this(0L, DEFAULT_MILLIS_PER_TICK);
  }

  public TickTimeManager(long initialMillis, long millisPerTick) {
    if (initialMillis < 0) {
      throw new IllegalArgumentException("initialMillis must be non-negative: " + initialMillis);
    }
    if (millisPerTick <= 0) {
      throw new IllegalArgumentException("millisPerTick must be positive: " + millisPerTick);
    }
    this.currentMillis = initialMillis;
    this.millisPerTick = millisPerTick;
  }

  @Override
  public long getMillis() {
    return currentMillis;
  }

  public long advance() {
    return advance(millisPerTick);
  }

  public long advance(long deltaMillis) {
    if (deltaMillis < 0) {
      throw new IllegalArgumentException("deltaMillis must be non-negative: " + deltaMillis);
    }
    currentMillis += deltaMillis;
    return currentMillis;
  }

  public long getTick() {
    return currentMillis / millisPerTick;
  }

  public long getMillisPerTick() {
    return millisPerTick;
  }
}
