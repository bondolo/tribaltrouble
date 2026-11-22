package com.oddlabs.tt.simulation.landscape;

import java.util.Random;

/// Deterministic LCG random for legacy simulation parity. Seed is fixed at 42; internal state is tracked for Merkle
/// hashing.
///
/// Replaced by [java.util.random.RandomGenerator] when the simulation migrates off [Random].
final class LegacyTTSimulationRandom extends Random {

  /// Fixed initial seed used by legacy simulation worlds.
  private static final long INITIAL_SEED = 42L;

  /// The LCG multiplier used by [Random].
  private static final long MULTIPLIER = 0x5DEECE66DL;

  /// The LCG addend (increment) used by [Random].
  private static final long ADDEND = 0xBL;

  /// The LCG mask (lower 48 bits).
  private static final long MASK = (1L << 48) - 1;

  private long seed;
  private double nextNextGaussian;
  private boolean haveNextNextGaussian;

  LegacyTTSimulationRandom() {
    super(INITIAL_SEED);
  }

  @Override
  protected int next(int bits) {
    seed = (seed * MULTIPLIER + ADDEND) & MASK;
    return (int) (seed >>> (48 - bits));
  }

  @Override
  public synchronized void setSeed(long newSeed) {
    super.setSeed(newSeed);
    seed = (newSeed ^ MULTIPLIER) & MASK;
    haveNextNextGaussian = false;
    nextNextGaussian = 0.0;
  }

  @Override
  public synchronized double nextGaussian() {
    if (haveNextNextGaussian) {
      haveNextNextGaussian = false;
      double cached = nextNextGaussian;
      nextNextGaussian = 0.0;
      return cached;
    }
    double v1;
    double v2;
    double s;
    do {
      v1 = 2 * nextDouble() - 1;
      v2 = 2 * nextDouble() - 1;
      s = v1 * v1 + v2 * v2;
    } while (s >= 1 || s == 0);
    double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
    nextNextGaussian = v2 * multiplier;
    haveNextNextGaussian = true;
    return v1 * multiplier;
  }

  /// Returns the current internal LCG seed for Merkle tree capture.
  long getSeed() {
    return seed;
  }

  /// Captures an immutable snapshot of this PRNG's internal state for Merkle tree hashing.
  RandomState captureState() {
    return new RandomState.LegacyLcg(seed, haveNextNextGaussian, nextNextGaussian);
  }

  /// Returns whether a second Gaussian value is currently cached for Merkle tree capture.
  boolean hasNextNextGaussian() {
    return haveNextNextGaussian;
  }

  /// Returns the cached second Gaussian value (0.0 when absent) for Merkle tree capture.
  double getNextNextGaussian() {
    return nextNextGaussian;
  }
}
