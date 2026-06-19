package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.LandscapeBoundsProvider;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A rock boulder supply that erupts from the ground.
 */
public final class RockSupply extends SupplyModel {
    private static final int INITIAL_SUPPLIES = 10;

    public RockSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        var rotation = ThreadLocalRandom.current().nextFloat((float) -Math.PI, (float) Math.PI);
        var fragmentIndex = ThreadLocalRandom.current().nextInt(LandscapeBoundsProvider.SUPPLY_FRAGMENT_COUNT);
        super(world, 2f, grid_x, grid_y, x, y, rotation, INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getRockBounds(fragmentIndex));
    }

    @Override
    public @NonNull SupplyType getSupplyType() {
        return SupplyType.ROCK;
    }

    @Override
    public @NonNull Supply respawn() {
        return new RockSupply(getWorld(), getGridX(), getGridY(), getPositionX(), getPositionY(), false);
    }

    @Override
    public void remove() {
        super.remove();
        getClientState(ModelClient.class).ifPresent(ModelClient::close);
    }
}
