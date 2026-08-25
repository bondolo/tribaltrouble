package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;

import java.util.Optional;

/**
 * A weapon factory for units that deal damage instantly to their targets
 * (e.g., melee units or non-projectile weapons).
 */
public final class InstantHitFactory extends WeaponFactory {

    public InstantHitFactory(float hit_chance, float range, float release_ratio) {
        super(hit_chance, range, release_ratio);
    }

    @Override
    protected void doAttack(boolean hit, Unit src, Selectable<?> target) {
        int damage = 1;

        if (target instanceof Building && target.getTemplate().getAbilities().hasAbilities(Abilities.ATTACK))
            damage = 6;
        else if (!hit)
            return;

        float dx = target.getPositionX() - src.getPositionX();
        float dy = target.getPositionY() - src.getPositionY();
        float dir_len_inv = 1f / (float) Math.hypot(dx, dy);
        if (target instanceof Unit unitTarget) {
            src.getWorld().getNotificationListener().onUnitAttack(unitTarget.getTemplate().getVisualType(), unitTarget
                    .getOwner().getRaceInfo().getRaceType(), target.getPositionX(), target.getPositionY(), target
                            .getPositionZ());
        }
        target.hit(damage, dx * dir_len_inv, dy * dir_len_inv, src.getOwner());
    }

    @Override
    public Optional<Class<? extends ThrowingWeapon>> getType() {
        return Optional.empty();
    }
}
