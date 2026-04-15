package com.oddlabs.tt.model;

import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public abstract non-sealed class AbstractAccessory extends Model implements Accessory {
    private final @NonNull SpriteKey sprite_renderer;
    private final @NonNull Unit unit;

    public AbstractAccessory(@NonNull Unit unit, @NonNull SpriteKey sprite_renderer) {
        super(unit.getOwner().getWorld());
        this.sprite_renderer = sprite_renderer;
        this.unit = unit;
        register();
    }

    @Override
    public final @NonNull SpriteKey getSpriteRenderer() {
        return sprite_renderer;
    }

    @Override
    public final @NonNull Unit getUnit() {
        return unit;
    }
}
