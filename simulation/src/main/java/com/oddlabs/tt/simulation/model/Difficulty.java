package com.oddlabs.tt.simulation.model;

import java.util.EnumSet;
import java.util.Set;

/// Game difficulty levels supporting legacy serialization and AI mapping.
public enum Difficulty {
  EASY(0, 1),
  NORMAL(1, 0),
  HARD(2, 2);

  private final int aiValue;
  private final int campaignValue;

  private static final Set<Difficulty> VALUES = EnumSet.allOf(Difficulty.class);

  Difficulty(int aiValue, int campaignValue) {
    this.aiValue = aiValue;
    this.campaignValue = campaignValue;
  }

  public int getAiValue() {
    return aiValue;
  }

  public int getCampaignValue() {
    return campaignValue;
  }

  public static Difficulty fromAiValue(int aiValue) {
    for (Difficulty diff : VALUES) {
      if (diff.aiValue == aiValue) {
        return diff;
      }
    }
    throw new IllegalArgumentException("Unknown AI difficulty value: " + aiValue);
  }

  public static Difficulty fromCampaignValue(int campaignValue) {
    for (Difficulty diff : VALUES) {
      if (diff.campaignValue == campaignValue) {
        return diff;
      }
    }
    throw new IllegalArgumentException("Unknown campaign difficulty value: " + campaignValue);
  }
}
