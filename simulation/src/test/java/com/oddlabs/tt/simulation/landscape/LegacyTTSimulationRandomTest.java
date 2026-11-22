package com.oddlabs.tt.simulation.landscape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/// Parity tests verifying [LegacyTTSimulationRandom] matches [Random] bit-for-bit.
@NullMarked
final class LegacyTTSimulationRandomTest {

  @Test
  void testBitExactParityWithJavaUtilRandom() {
    LegacyTTSimulationRandom simRandom = new LegacyTTSimulationRandom();
    Random refRandom = new Random(42L);

    long initialSeed = simRandom.getSeed();
    for (int i = 0; i < 100; i++) {
      assertEquals(refRandom.nextInt(), simRandom.nextInt());
      assertEquals(refRandom.nextInt(17), simRandom.nextInt(17));
      assertEquals(refRandom.nextLong(), simRandom.nextLong());
      assertEquals(refRandom.nextFloat(), simRandom.nextFloat());
      assertEquals(refRandom.nextDouble(), simRandom.nextDouble());
      assertEquals(refRandom.nextBoolean(), simRandom.nextBoolean());
      assertEquals(refRandom.nextGaussian(), simRandom.nextGaussian());
      assertEquals(refRandom.nextGaussian(1.5, 2.5), simRandom.nextGaussian(1.5, 2.5));
    }
    assertNotEquals(initialSeed, simRandom.getSeed());

    simRandom.setSeed(123456L);
    refRandom.setSeed(123456L);
    for (int i = 0; i < 50; i++) {
      assertEquals(refRandom.nextInt(), simRandom.nextInt());
      assertEquals(refRandom.nextGaussian(), simRandom.nextGaussian());
      assertEquals(refRandom.nextGaussian(1.5, 2.5), simRandom.nextGaussian(1.5, 2.5));
    }
  }

  @Test
  void testGaussianCacheStateTracking() {
    LegacyTTSimulationRandom simRandom = new LegacyTTSimulationRandom();
    assertFalse(simRandom.hasNextNextGaussian());
    assertEquals(0.0, simRandom.getNextNextGaussian());

    simRandom.nextGaussian();
    assertTrue(simRandom.hasNextNextGaussian());
    long seedAfterFirst = simRandom.getSeed();

    simRandom.nextGaussian();
    assertEquals(seedAfterFirst, simRandom.getSeed(), "Consuming cached Gaussian must not advance LCG seed");
    assertFalse(simRandom.hasNextNextGaussian());
    assertEquals(0.0, simRandom.getNextNextGaussian());

    simRandom.nextGaussian();
    assertTrue(simRandom.hasNextNextGaussian());

    simRandom.setSeed(99L);
    assertFalse(simRandom.hasNextNextGaussian());
    assertEquals(0.0, simRandom.getNextNextGaussian());
  }
}
