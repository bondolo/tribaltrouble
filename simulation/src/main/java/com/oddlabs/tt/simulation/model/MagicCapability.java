package com.oddlabs.tt.simulation.model;

import java.util.EnumMap;
import org.jspecify.annotations.Nullable;

/// Tracks per-spell magic energy accumulation and cast history for magic-capable units.
public final class MagicCapability {
  private final EnumMap<MagicType, Float> energy = new EnumMap<>(MagicType.class);
  private @Nullable MagicType lastMagicType;

  public float getEnergy(MagicType type) {
    return energy.getOrDefault(type, 0f);
  }

  public float getProgress(MagicType type) {
    return getEnergy(type) / type.getMaxEnergy();
  }

  public boolean isReady(MagicType type) {
    return getEnergy(type) == type.getMaxEnergy();
  }

  public void increaseEnergy(MagicType type, float amount) {
    float nextVal = Math.min(type.getMaxEnergy(), getEnergy(type) + amount);
    energy.put(type, nextVal);
  }

  public void maxEnergy(MagicType type) {
    energy.put(type, type.getMaxEnergy());
  }

  public void rechargeAll(Iterable<MagicType> magics, float dt) {
    for (MagicType magic : magics) {
      increaseEnergy(magic, dt);
    }
  }

  public void consumeAll(MagicType castType) {
    energy.clear();
    lastMagicType = castType;
  }

  public @Nullable MagicType getLastMagicType() {
    return lastMagicType;
  }
}
