package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

/**
 * A throwing spear weapon made of rock.
 */
public final class RockSpearWeapon extends DirectedThrowingWeapon {
    private static final float METERS_PER_SECOND = 20f; //multiplied by meters/second (in 2D)

    public RockSpearWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target,
            @NonNull SpriteKey sprite_renderer, @NonNull AudioFile throw_sound,
            @NonNull AudioFile @NonNull [] hit_sounds) {
        super(hit, src, target, sprite_renderer, throw_sound, hit_sounds);
    }

    @Override
    protected float getMetersPerSecond() {
        return METERS_PER_SECOND;
    }

    @Override
    protected int getDamage() {
        return 1;
    }
}
