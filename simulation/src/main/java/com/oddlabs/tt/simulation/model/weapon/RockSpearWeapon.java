package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;

/// A throwing spear weapon made of rock.
public final class RockSpearWeapon extends DirectedThrowingWeapon {
  public RockSpearWeapon(boolean hit, Unit src, Selectable<?> target) {
    super(hit, src, target);
  }

  @Override
  public WeaponTier getWeaponTier() {
    return WeaponTier.ROCK;
  }

  @Override
  protected int getDamage() {
    return 1;
  }
}
