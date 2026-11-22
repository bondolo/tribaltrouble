package com.oddlabs.tt.simulation.landscape;

/// Immutable snapshot of simulation PRNG internal state for deterministic Merkle hashing and diffing.
public sealed interface RandomState {

  /// State snapshot for the 48-bit legacy LCG (`java.util.Random`) including its Box-Muller Gaussian cache.
  record LegacyLcg(long seed, boolean haveNextNextGaussian, double nextNextGaussian) implements RandomState {}
}
