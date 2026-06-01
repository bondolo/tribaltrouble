package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import com.oddlabs.util.Color;

/**
 * Abstract base class for non-tree harvestable resources in the world such as rocks and iron.
 */
public abstract sealed class SupplyModel extends AccessorizableModel implements Supply, Target permits IronSupply,
        RubberSupply, RockSupply {
    private static final float SPAWN_OFFSET_Z = -2f;

    @Override
    public abstract @NonNull SpriteKey getStatusSprite(@NonNull RacesResources resources);

    private final @NonNull SpriteKey sprite_renderer;

    private final float size;
    private final float rotation;

    private float offset_z = 0;

    private int grid_x;
    private int grid_y;

    private final int max_supplies;
    private int num_supplies;
    private int hit_counter = 0;

    @SuppressWarnings("unchecked")
    public SupplyModel(@NonNull World world, @NonNull SpriteKey sprite_renderer, float size, int grid_x, int grid_y,
            float x, float y, float rotation, int num_supplies, boolean increase_count) {
        super(world);
        this.sprite_renderer = sprite_renderer;
        this.size = size;
        this.grid_x = grid_x;
        this.grid_y = grid_y;
        this.rotation = rotation;
        this.num_supplies = num_supplies;
        this.max_supplies = num_supplies;
        setPosition(x, y);
        updateBounds();
        world.getNotificationListener().registerTarget(this);
        UnitGrid unit_grid = world.getUnitGrid();
        unit_grid.occupyGrid(grid_x, grid_y, this);
        Region region = unit_grid.getRegion(grid_x, grid_y);
        region.registerObject((Class<SupplyModel>) getClass(), this);
        register();
        reinsert();
        if (increase_count)
            world.getSupplyManager(getClass()).newSupply();
    }

    public final float getRotation() {
        return rotation;
    }

    @Override
    public void animateSpawn(float t, float progress) {
        offset_z = SPAWN_OFFSET_Z * (1 - progress);
        reinsert();
    }

    @Override
    public void spawnComplete() {
    }

    public Color.@Nullable Linear getSpawnColorTint() {
        return null;
    }

    public Color.@Nullable Linear getCrackDecalColor() {
        return null;
    }

    public float getCrackDecalOpacity() {
        return 0.0f;
    }

    public float getCrackDecalDiameter() {
        return getSize();
    }

    public float getCrackDecalPattern() {
        return 0.0f;
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
            region.unregisterObject((Class<SupplyModel>) getClass(), this);
            remove();
            getWorld().getSupplyManager(getClass()).emptySupply(this);
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

    @Override
    public float getShadowDiameter() {
        return 7.0f * getSupplyRatio();
    }

    private boolean showShadow = true;

    protected final void setShowShadow(boolean showShadow) {
        this.showShadow = showShadow;
    }

    @Override
    public float getShadowOpacity() {
        return !showShadow ? 0.0f : 0.5f * getSupplyRatio();
    }

    private float getSupplyRatio() {
        return max_supplies > 0 ? (float) num_supplies / max_supplies : 0.0f;
    }

    @Override
    public float getShadowVerticalCenter() {
        return 0.3f;
    }

    @Override
    public final float getNoDetailSize() {
        throw new IllegalStateException();
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
        return offset_z + calculateSlopeOffset();
    }

    protected float calculateSlopeOffset() {
        // Check surrounding heights to lift object on slopes
        float r = getSize() * 0.2f;
        float x = getPositionX();
        float y = getPositionY();
        var hm = getWorld().getHeightMap();

        float h_center = hm.getNearestHeight(x, y);
        float h_max = h_center;

        // Axis-aligned
        h_max = Math.max(h_max, hm.getNearestHeight(x + r, y));
        h_max = Math.max(h_max, hm.getNearestHeight(x - r, y));
        h_max = Math.max(h_max, hm.getNearestHeight(x, y + r));
        h_max = Math.max(h_max, hm.getNearestHeight(x, y - r));

        // Diagonals
        float d = r * 0.707f;
        h_max = Math.max(h_max, hm.getNearestHeight(x + d, y + d));
        h_max = Math.max(h_max, hm.getNearestHeight(x - d, y + d));
        h_max = Math.max(h_max, hm.getNearestHeight(x + d, y - d));
        h_max = Math.max(h_max, hm.getNearestHeight(x - d, y - d));

        return Math.max(0f, h_max - h_center);
    }

    @Override
    public final @NonNull SpriteKey getSpriteRenderer() {
        return sprite_renderer;
    }

    @Override
    protected @NonNull BoundingBox @NonNull [] getLocalBounds() {
        return sprite_renderer.bounds();
    }

    @Override
    public int getPenalty() {
        return Occupant.STATIC;
    }
}
