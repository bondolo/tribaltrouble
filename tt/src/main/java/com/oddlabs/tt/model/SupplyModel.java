package com.oddlabs.tt.model;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.UnitGrid;
import org.jspecify.annotations.NonNull;

/**
 * Abstract base class for non-tree harvestable resources in the world such as rocks and iron.
 */
public abstract sealed class SupplyModel extends Model implements Supply, Target, Animated permits IronSupply,
        RubberSupply, RockSupply {
    /** This is also known to be a SpriteKey */
    private final @NonNull BoundsProvider boundsProvider;

    private final float size;
    private final float rotation;

    private int grid_x;
    private int grid_y;

    private final int max_supplies;
    private int num_supplies;
    private int hit_counter = 0;

    /**
     * Constructs a SupplyModel instance, which represents a supply object in the game world.
     *
     * @param world world instance where this supply model exists; must not be null
     * @param size size of the supply model
     * @param grid_x x-coordinate of the grid position
     * @param grid_y y-coordinate of the grid position
     * @param x x-coordinate of the initial position
     * @param y y-coordinate of the initial position
     * @param rotation rotation of the supply model in radians
     * @param num_supplies initial number of supplies in this model
     * @param increase_count a flag indicating whether to notify the supply manager to increment the supply count
     * @param boundsProvider a provider for bounds of the supply model; must not be null. Also, sneakily the SpriteKey
     */
    public SupplyModel(@NonNull World world, float size, int grid_x, int grid_y,
            float x, float y, float rotation, int num_supplies, boolean increase_count,
            @NonNull BoundsProvider boundsProvider) {
        super(world);
        this.boundsProvider = boundsProvider;
        this.size = size;
        this.grid_x = grid_x;
        this.grid_y = grid_y;
        this.rotation = rotation;
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


    /** {@return radians to rotate the model about the z-axis} */
    public final float getRotation() {
        return rotation;
    }

    @Override
    public abstract @NonNull SupplyType getSupplyType();

    @Override
    public void register() {
        super.register();
        getWorld().getAnimationManagerGameTime().registerAnimation(this);
    }

    @Override
    public void remove() {
        getWorld().getAnimationManagerGameTime().removeAnimation(this);
        super.remove();
    }

    @Override
    public void animate(float t) {
        animateClientState(t);
    }

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
        float r = getShadowDiameter() * 0.5f;
        // Expand bounds by shadow radius to prevent culling of the shadow
        bmin_x -= r;
        bmin_y -= r;
        bmax_x += r;
        bmax_y += r;
    }

    public float getShadowDiameter() {
        return 7.0f * getSupplyRatio();
    }

    private float getSupplyRatio() {
        return max_supplies > 0 ? (float) num_supplies / max_supplies : 0.0f;
    }

    @Override
    public final float getSize() {
        return size;
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

    public @NonNull BoundsProvider getBoundsProvider() {
        return boundsProvider;
    }

    @Override
    protected @NonNull BoundingBox @NonNull [] getLocalBounds() {
        return boundsProvider.bounds();
    }

    @Override
    public int getPenalty() {
        return Occupant.STATIC;
    }
}
