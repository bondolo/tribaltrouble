package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.state.Idling;
import com.oddlabs.tt.simulation.state.UnitState;

/// Garrison mount descriptor for a warrior stationed atop a defensive tower.
public record TowerMount(Building host, float offsetZ, float rangeBonus) implements UnitMount {
  public static final float ATTACK_SPEED_MULTIPLIER = 3f;
  public static final int STATUS_VALUE_MULTIPLIER = 3;

  public TowerMount(Building host) {
    this(host, host.getTemplate().getMountOffset(), MountUnitContainer.ATTACK_RANGE_INCREASE);
  }

  @Override
  public boolean forwardsDamageToHost() {
    return true;
  }

  @Override
  public float attackSpeedMultiplier() {
    return ATTACK_SPEED_MULTIPLIER;
  }

  @Override
  public int statusValueMultiplier() {
    return STATUS_VALUE_MULTIPLIER;
  }

  @Override
  public UnitState mountedDefaultState() {
    return new Idling(AttackScanFilter.TOWER_RANGE, false);
  }
}
