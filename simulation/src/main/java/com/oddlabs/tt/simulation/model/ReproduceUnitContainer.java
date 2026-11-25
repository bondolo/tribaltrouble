package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.Nullable;

/// Manages passive peon reproduction and chieftain training ticks inside quarters.
public final class ReproduceUnitContainer extends UnitContainer {
  private final Building building;
  private final ChieftainContainer chieftainContainer;

  private float unit_reproduction = 0f;

  public ReproduceUnitContainer(Building building, ChieftainContainer chieftainContainer) {
    super(building.getOwner().getWorld().getMaxUnitCount());
    this.building = building;
    this.chieftainContainer = chieftainContainer;
  }

  public float getBuildProgress() {
    return chieftainContainer.isTraining() ? 0f : unit_reproduction;
  }

  public void resetProgress() {
    unit_reproduction = 0f;
  }

  @Override
  public void enter(Unit unit) {
    assert canEnter(unit);
    unit.removeNow();
    increaseSupply(1);
  }

  @Override
  public boolean canEnter(Unit unit) {
    return !unit.getAbilities().hasAbilities(Abilities.THROW) && getTotalSupplies() != getMaxSupplyCount();
  }

  private int getTotalSupplies() {
    //		return getNumSupplies() + building.getBuildSupplyContainer(Unit.class).getNumSupplies() == getMaxSupplyCount();
    return getNumSupplies() + getNumPreparing();
  }

  @Override
  public @Nullable Unit exit() {
    assert getNumSupplies() > 0;
    increaseSupply(-1);
    return null;
  }

  @Override
  public int increaseSupply(int amount) {
    int result = building.getOwner().getUnitCountContainer().increaseSupply(amount);
    assert result == amount : "result = " + result + " | amount = " + amount;
    return super.increaseSupply(amount);
  }

  @Override
  public void animate(float t) {
    if ((building.getOwner().getUnitCountContainer().getNumSupplies() < getMaxSupplyCount()
            && getTotalSupplies() != getMaxSupplyCount())
        || chieftainContainer.isTraining()) {
      float units = Math.max(getNumSupplies(), .5f);
      unit_reproduction += ((1f / 11f) * Math.pow(units, 1f / 3f)) * t;
      while (unit_reproduction >= 1f) {
        unit_reproduction -= 1f;
        if (chieftainContainer.isTraining()) {
          chieftainContainer.progress();
        } else {
          increaseSupply(1);
        }
      }
    } else {
      unit_reproduction = 0;
    }
  }
}
