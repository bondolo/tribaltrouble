package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class Plants extends SceneryModel {
    public Plants(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull SpriteKey sprite_renderer) {
        super(world, x, y, dir_x, dir_y, sprite_renderer);
        world.registerPlant(this);
    }

    @Override
    protected void doRegister() {
        register();
        reinsert();
    }
}
