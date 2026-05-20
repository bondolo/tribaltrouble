package com.oddlabs.tt.model;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class Plants extends SceneryModel {
    public Plants(@NonNull World world, float x, float y, float dir_x, float dir_y, @NonNull SpriteKey sprite_renderer) {
        super(world, x, y, dir_x, dir_y, sprite_renderer);
    }

    @Override
    protected void doRegister() {
        if (Globals.INSERT_PLANTS[Renderer.getRenderer().getSettings().graphic_detail]) {
            register();
            reinsert();
        }
    }
}
