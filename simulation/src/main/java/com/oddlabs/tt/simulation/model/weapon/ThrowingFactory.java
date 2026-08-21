package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;

import java.util.Optional;

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

    private final Class<W> weapon_type;
    private final WeaponConstructor<W> weapon_constructor;

    public ThrowingFactory(Class<W> weapon_type, WeaponConstructor<W> weapon_constructor,
            float hit_chance, float range, float release_ratio) {
        super(hit_chance, range, release_ratio);
        this.weapon_type = weapon_type;
        this.weapon_constructor = weapon_constructor;
    }

    @Override
    protected void doAttack(boolean hit, Unit src, Selectable<?> target) {
        weapon_constructor.create(hit, src, target);
    }

    @Override
    public Optional<Class<? extends ThrowingWeapon>> getType() {
        return Optional.of(weapon_type);
    }
}
