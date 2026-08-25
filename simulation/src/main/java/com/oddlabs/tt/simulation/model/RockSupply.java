package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A rock boulder supply that erupts from the ground.
 */
public final class RockSupply extends SupplyModel {
    private static final int INITIAL_SUPPLIES = 10;

    public RockSupply(World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        var fragmentIndex = ThreadLocalRandom.current().nextInt(LandscapeBoundsProvider.SUPPLY_FRAGMENT_COUNT);
        super(world, grid_x, grid_y, x, y, INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getRockBounds(fragmentIndex));
    }

    @Override
    public SupplyType getSupplyType() {
        return SupplyType.ROCK;
    }

    @Override
    public Supply respawn() {
        return new RockSupply(getWorld(), getGridX(), getGridY(), getPositionX(), getPositionY(), false);
    }

    @Override
    public void remove() {
        super.remove();
        getWorld().getNotificationListener().onModelRemoved(this);
    }
}
