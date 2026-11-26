package com.oddlabs.tt.simulation.model;

import static com.oddlabs.tt.simulation.model.Ability.TARGET;
import static com.oddlabs.tt.simulation.model.Ability.THROW;

import org.jspecify.annotations.Nullable;

/// Unit container mounted atop a defensive tower holding a single ranged warrior.
public final class MountUnitContainer extends UnitContainer {
  public static final float ATTACK_RANGE_INCREASE = 8f;

  private final Building building;
  private @Nullable Unit unit;

  public MountUnitContainer(Building building) {
    super(1);
    this.building = building;
  }

  @Override
  public void enter(Unit unit) {
    this.unit = unit;
    unit.mount(new TowerMount(building));
    increaseSupply(1);
    building.getAbilities().add(TARGET);
  }

  @Override
  public Unit exit() {
    assert unit != null;
    unit.unmount();
    Unit result = unit;
    unit = null;
    increaseSupply(-1);
    building.getAbilities().remove(TARGET);
    return result;
  }

  @Override
  public boolean canEnter(Unit unit) {
    return !isSupplyFull() && unit.hasAbility(THROW);
  }

  public @Nullable Unit getUnit() {
    return unit;
  }
}
