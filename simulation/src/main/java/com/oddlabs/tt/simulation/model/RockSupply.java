package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A rock boulder supply that erupts from the ground.
 */
public final class RockSupply extends SupplyModel {
    private static final int INITIAL_SUPPLIES = 10;

    private final int fragmentIndex;

    public RockSupply(World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        var fragmentIndex = ThreadLocalRandom.current().nextInt(LandscapeBoundsProvider.SUPPLY_FRAGMENT_COUNT);
        this(world, grid_x, grid_y, x, y, increase, fragmentIndex);
    }

    private RockSupply(World world, int grid_x, int grid_y, float x, float y, boolean increase,
            int fragmentIndex) {
        super(world, grid_x, grid_y, x, y, INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getRockBounds(fragmentIndex));
        this.fragmentIndex = fragmentIndex;
    }

    public int getFragmentIndex() {
        return fragmentIndex;
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
