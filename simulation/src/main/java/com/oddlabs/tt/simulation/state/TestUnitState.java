package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.EntityActivity;

/// Package-private non-sealed extension hook allowing unit-test stubs to implement `UnitState`.
non-sealed interface TestUnitState extends UnitState {
  default EntityActivity getActivity() {
    return EntityActivity.OTHER;
  }

  default boolean isBlocking() {
    return false;
  }
}
