package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.Assets;
import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.UnitTemplate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A weapon factory for units that deal damage instantly to their targets 
 * (e.g., melee units or non-projectile weapons).
 */
public final class InstantHitFactory extends WeaponFactory {
    private final @NonNull AudioFile @NonNull [] sounds;

    public InstantHitFactory(float hit_chance, float range, float release_ratio, @NonNull AudioFile @NonNull [] sounds) {
        super(hit_chance, range, release_ratio);
        this.sounds = sounds;
    }

    @Override
    protected void doAttack(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target) {
        int damage = 1;
        if (target instanceof Building && target.getTemplate().getAbilities().hasAbilities(Abilities.ATTACK))
            damage = 6;
        else if (!hit)
            return;

        float dx = target.getPositionX() - src.getPositionX();
        float dy = target.getPositionY() - src.getPositionY();
        float dir_len_inv = 1f / (float) Math.hypot(dx, dy);
        if (target instanceof Unit) {
            World world = src.getOwner().getWorld();
            var params = new AudioParameters(
                    sounds[world.getRandom().nextInt(sounds.length)], Assets.AUDIO_RANK_WEAPON_HIT,
                    Assets.AUDIO_DISTANCE_WEAPON_HIT, Assets.AUDIO_GAIN_WEAPON_HIT, Assets.AUDIO_RADIUS_WEAPON_HIT,
                    1f + (world.getRandom().nextFloat() - .5f) * ((UnitTemplate) target.getTemplate()).getDeathPitch());
            world.getAudio().newAudio(target.getPositionX(), target.getPositionY(), target.getPositionZ(), params);
        }
        target.hit(damage, dx * dir_len_inv, dy * dir_len_inv, src.getOwner());
    }

    @Override
    public @Nullable Class<? extends ThrowingWeapon> getType() {
        return null;
    }
}
