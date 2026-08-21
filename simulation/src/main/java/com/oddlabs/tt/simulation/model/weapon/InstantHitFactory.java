package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.ModelClient;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitTemplate;

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
        if (target instanceof Unit) {
            float pitchRange = ((UnitTemplate) target.getTemplate()).getDeathPitch();
            src.getClientState(ModelClient.class).ifPresent(client -> {
                client.onMeleeHit(target.getPositionX(), target.getPositionY(), target.getPositionZ(), pitchRange);
            });
        }
        target.hit(damage, dx * dir_len_inv, dy * dir_len_inv, src.getOwner());
    }

    @Override
    public Optional<Class<? extends ThrowingWeapon>> getType() {
        return Optional.empty();
    }
}
