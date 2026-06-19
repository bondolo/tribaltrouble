package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.LandscapeBoundsProvider;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * An iron boulder supply that falls from the sky as a meteor.
 */
public final class IronSupply extends SupplyModel {
    private static final int INITIAL_SUPPLIES = 10;

    private final int fragmentIndex;

    public IronSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        var fragmentIndex = ThreadLocalRandom.current().nextInt(LandscapeBoundsProvider.SUPPLY_FRAGMENT_COUNT);
        this(world, grid_x, grid_y, x, y, increase, fragmentIndex);
    }

    private IronSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase,
            int fragmentIndex) {
        super(world, 2f, grid_x, grid_y, x, y, ThreadLocalRandom.current().nextFloat((float) -Math.PI,
                (float) Math.PI), INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getIronBounds(fragmentIndex));
        this.fragmentIndex = fragmentIndex;
    }

    public int getFragmentIndex() {
        return fragmentIndex;
    }

    @Override
    public @NonNull SupplyType getSupplyType() {
        return SupplyType.IRON;
    }

    @Override
    public @NonNull Supply respawn() {
        return new IronSupply(getWorld(), getGridX(), getGridY(), getPositionX(), getPositionY(), false);
    }

    @Override
    public void remove() {
        super.remove();
        getClientState(ModelClient.class).ifPresent(ModelClient::close);
    }
}
