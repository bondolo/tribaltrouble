package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.state.UnitState;

/// Capability describing a unit's garrisoned or embarked mount relationship with a host building.
public interface UnitMount {
  Building host();

  float offsetZ();

  float rangeBonus();

  boolean forwardsDamageToHost();

  float attackSpeedMultiplier();

  int statusValueMultiplier();

  UnitState mountedDefaultState();

  default float unmountPositionX() {
    return host().getPositionX();
  }

  default float unmountPositionY() {
    return host().getPositionY();
  }
}
