package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.World;

/**
 * Plants scenery model, representing ground vegetation.
 */
public final class Plants extends SceneryModel {
    private final Terrain terrain;
    private final int index;

    public Plants(World world, float x, float y, float dir_x, float dir_y,
            Terrain terrain, int index) {
        super(world, x, y, dir_x, dir_y, world.getLandscapeResources().getPlantBounds(terrain, index));
        this.terrain = terrain;
        this.index = index;
        world.registerPlant(this);
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected void doRegister() {
        register();
        reinsert();
    }
}
