package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.simulation.landscape.World;

/**
 * Plants scenery model, representing ground vegetation.
 */
public final class Plants extends SceneryModel {
    public Plants(World world, float x, float y, float dir_x, float dir_y,
            BoundsProvider boundsProvider) {
        super(world, x, y, dir_x, dir_y, boundsProvider);
        world.registerPlant(this);
    }


    @Override
    protected void doRegister() {
        register();
        reinsert();
    }
}
