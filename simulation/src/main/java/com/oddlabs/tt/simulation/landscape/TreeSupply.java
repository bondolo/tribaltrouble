package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.Region;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.model.Target;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * A harvestable tree resource in the game world.
 * Provides wood supplies when harvested by peon units.
 */
public final class TreeSupply extends AbstractTreeGroup implements Supply, Target {
    private static final int INITIAL_SUPPLIES = 10;

    private final Matrix4f matrix;
    private final TreeType tree_type;
    private final float x;
    private final float y;
    private final int grid_x;
    private final int grid_y;
    private final int grid_size;
    private final float size;
    private final World world;

    private int num_supplies = INITIAL_SUPPLIES;
    private int hit_counter = 0;

    public TreeSupply(World world, @Nullable AbstractTreeGroup parent, float x, float y, int grid_x,
            int grid_y, int grid_size, float size, Matrix4f matrix, TreeType tree_type) {
        super(parent);
        this.world = world;
        this.x = x;
        this.y = y;
        this.grid_x = grid_x;
        this.grid_y = grid_y;
        this.grid_size = grid_size;
        this.size = size;
        this.tree_type = tree_type;
        this.matrix = matrix;
        checkBoundsX(x - size);
        checkBoundsX(x + size);
        checkBoundsY(y - size);
        checkBoundsY(y + size);
        checkBoundsZ(matrix.m32());
        checkBoundsZ(matrix.m32() + 15f);
        if (world.getUnitGrid().getOccupant(grid_x, grid_y) == null)
            occupyTree();
        world.getSupplyManager(getSupplyType()).newSupply();
    }

    public void updateBounds(BoundingBox modelBounds) {
        resetBounds();
        Vector4f src = new Vector4f();
        Vector4f dest = new Vector4f();
        float[] corners = new float[]{
                modelBounds.bmin_x, modelBounds.bmin_y, modelBounds.bmin_z,
                modelBounds.bmax_x, modelBounds.bmin_y, modelBounds.bmin_z,
                modelBounds.bmax_x, modelBounds.bmax_y, modelBounds.bmin_z,
                modelBounds.bmin_x, modelBounds.bmax_y, modelBounds.bmin_z,
                modelBounds.bmin_x, modelBounds.bmin_y, modelBounds.bmax_z,
                modelBounds.bmax_x, modelBounds.bmin_y, modelBounds.bmax_z,
                modelBounds.bmax_x, modelBounds.bmax_y, modelBounds.bmax_z,
                modelBounds.bmin_x, modelBounds.bmax_y, modelBounds.bmax_z
        };
        for (int i = 0; i < corners.length; i += 3) {
            src.set(corners[i], corners[i + 1], corners[i + 2], 1f);
            matrix.transform(src, dest);
            checkBoundsX(dest.x);
            checkBoundsY(dest.y);
            checkBoundsZ(dest.z);
        }
    }

    @Override
    public SupplyType getSupplyType() {
        return SupplyType.WOOD;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public TreeSupply respawn() {
        occupyTree();
        num_supplies = INITIAL_SUPPLIES;
        return this;
    }

    @Override
    public String toString() {
        return "Tree at " + grid_x + " " + grid_y + " isEmpty() " + isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void occupyTree() {
        UnitGrid grid = world.getUnitGrid();
        world.registerTarget(this);
        Region region = grid.getRegion(getGridX(), getGridY());
        region.registerObject(TreeSupply.class, this);
        for (int y = 0; y < grid_size; y++) {
            int occ_y = grid_y + y - (grid_size - 1) / 2;
            for (int x = 0; x < grid_size; x++) {
                int occ_x = grid_x + x - (grid_size - 1) / 2;
                if (!grid.isGridOccupied(occ_x, occ_y)) {
                    assert !(grid.getOccupant(occ_x, occ_y) instanceof TreeSupply) : "Trees placed too close";
                }
            }
        }
        grid.occupyGrid(grid_x, grid_y, this);
    }

    @SuppressWarnings("unchecked")
    private void unoccupyTree() {
        UnitGrid grid = world.getUnitGrid();
        world.unregisterTarget(this);
        Region region = grid.getRegion(grid_x, grid_y);
        region.unregisterObject(TreeSupply.class, this);
        grid.freeGrid(grid_x, grid_y, this);
    }

    @Override
    public float getSize() {
        return size;
    }

    @Override
    public float getPositionX() {
        return x;
    }

    @Override
    public float getPositionY() {
        return y;
    }

    @Override
    public int getGridX() {
        return grid_x;
    }

    @Override
    public int getGridY() {
        return grid_y;
    }

    @Override
    public int getPenalty() {
        return Occupant.STATIC;
    }

    @Override
    public boolean isEmpty() {
        return num_supplies == 0;
    }

    @Override
    public boolean hit() {
        hit_counter++;
        if (hit_counter == Supply.HITS_PER_HARVEST) {
            hit_counter = 0;
            decreaseSupply();
            return true;
        }
        return false;
    }

    @Override
    public boolean isDead() {
        return isEmpty();
    }

    private void decreaseSupply() {
        num_supplies--;
        if (isEmpty()) {
            unoccupyTree();
            world.getSupplyManager(getSupplyType()).emptySupply(this);
            world.getNotificationListener().treeFelled(this);
        }
    }

    public TreeType getTreeType() {
        return tree_type;
    }

    @Override
    public boolean initBounds() {
        super.initBounds();
        return true;
    }

    public Matrix4f getMatrix() {
        return matrix;
    }
}
