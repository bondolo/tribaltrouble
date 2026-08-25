package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.Region;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;

/**
 * Abstract base class for non-tree harvestable resources in the world such as rocks and iron.
 */
public abstract sealed class SupplyModel extends Model implements Supply, Target permits IronSupply,
        RubberSupply, RockSupply {
    /** This is also known to be a SpriteKey */
    private final BoundsProvider boundsProvider;

    private int grid_x;
    private int grid_y;

    private final int max_supplies;
    private int num_supplies;
    private int hit_counter = 0;

    public SupplyModel(World world, int grid_x, int grid_y,
            float x, float y, int num_supplies, boolean increase_count,
            BoundsProvider boundsProvider) {
        super(world);
        this.boundsProvider = boundsProvider;
        this.grid_x = grid_x;
        this.grid_y = grid_y;
        this.num_supplies = num_supplies;
        this.max_supplies = num_supplies;
        super.setPosition(x, y); // Set raw coordinates without triggering height lookup yet
        world.getNotificationListener().registerTarget(this);
        UnitGrid unit_grid = world.getUnitGrid();
        unit_grid.occupyGrid(grid_x, grid_y, this);
        Region region = unit_grid.getRegion(grid_x, grid_y);
        @SuppressWarnings("unchecked") var supplyClass = (Class<Supply>) getSupplyType().getSupplyClass();
        region.registerObject(supplyClass, this);
        register();
        if (increase_count)
            world.getSupplyManager(getSupplyType()).newSupply();
    }

    @Override
    public abstract SupplyType getSupplyType();

    @Override
    public final boolean isEmpty() {
        return num_supplies == 0;
    }

    @Override
    public boolean hit() {
        hit_counter++;
        if (hit_counter == Supply.HITS_PER_HARVEST) {
            hit_counter = 0;
            decreaseSupply();
            return true;
        } else
            return false;
    }

    @Override
    public final boolean isDead() {
        return isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void decreaseSupply() {
        num_supplies--;
        if (isEmpty()) {
            UnitGrid unit_grid = getWorld().getUnitGrid();
            unit_grid.freeGrid(grid_x, grid_y, this);
            getWorld().getNotificationListener().unregisterTarget(this);
            Region region = unit_grid.getRegion(grid_x, grid_y);
            @SuppressWarnings("unchecked") var supplyClass = (Class<Supply>) getSupplyType().getSupplyClass();
            region.unregisterObject(supplyClass, this);
            remove();
            getWorld().getSupplyManager(getSupplyType()).emptySupply(this);
        }
    }

    @Override
    protected void updateBounds() {
        super.updateBounds();
        float r = 3.5f * getSupplyRatio();
        // Expand bounds by shadow radius to prevent culling of the shadow
        bmin_x -= r;
        bmin_y -= r;
        bmax_x += r;
        bmax_y += r;
    }

    public final float getSupplyRatio() {
        return max_supplies > 0 ? (float) num_supplies / max_supplies : 0.0f;
    }

    @Override
    public final float getNoDetailSize() {
        throw new IllegalStateException();
    }

    @Override
    public final float getSize() {
        return 2.0f;
    }

    @Override
    public final int getGridX() {
        return grid_x;
    }

    @Override
    public final int getGridY() {
        return grid_y;
    }

    public void setGridPosition(int grid_x, int grid_y) {
        assert !isDead();
        this.grid_x = grid_x;
        this.grid_y = grid_y;
    }

    @Override
    public float getOffsetZ() {
        return getSlopeOffset();
    }

    protected final float getSlopeOffset() {
        return getSlopeOffset(getSize() * 0.2f);
    }

    public BoundsProvider getBoundsProvider() {
        return boundsProvider;
    }

    @Override
    protected BoundingBox[] getLocalBounds() {
        return boundsProvider.bounds();
    }

    @Override
    public int getPenalty() {
        return Occupant.STATIC;
    }
}
