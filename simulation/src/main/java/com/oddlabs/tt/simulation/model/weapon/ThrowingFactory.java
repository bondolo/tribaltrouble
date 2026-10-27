package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;

/**
 * A factory for creating throwing weapons.
 *
 * @param <W> the type of throwing weapon produced by this factory
 */
public final class ThrowingFactory<W extends ThrowingWeapon> extends WeaponFactory {
    @FunctionalInterface
    public interface WeaponConstructor<W extends ThrowingWeapon> {
        W create(boolean hit, Unit src, Selectable<?> target);
    }

    private final WeaponTier tier;
    private final WeaponConstructor<W> weapon_constructor;

    public ThrowingFactory(WeaponTier tier, WeaponConstructor<W> weapon_constructor,
            float hit_chance, float range, float release_ratio) {
        super(hit_chance, range, release_ratio);
        this.tier = tier;
        this.weapon_constructor = weapon_constructor;
    }

    @Override
    protected void doAttack(boolean hit, Unit src, Selectable<?> target) {
        weapon_constructor.create(hit, src, target);
    }

    @Override
    public WeaponTier getTier() {
        return tier;
    }
}
