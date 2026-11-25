package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;

/// A throwing spear weapon made of iron.
public final class IronSpearWeapon extends DirectedThrowingWeapon {
  public IronSpearWeapon(boolean hit, Unit src, Selectable<?> target) {
    super(hit, src, target);
  }

  @Override
  public WeaponTier getWeaponTier() {
    return WeaponTier.IRON;
  }

  @Override
  protected int getDamage() {
    return 2;
  }
}
