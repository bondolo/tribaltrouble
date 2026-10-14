package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.TreePlacement;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RubberGroup {
    private static final int MIN_CHICKENS_PER_GROUP = 3;
    private static final int MAX_CHICKENS_PER_GROUP = 7;

    private final World world;
    private final List<Supply> supplies = new ArrayList<>();

    public RubberGroup(World world) {
        this.world = world;
        TreePlacement group_position = getGroupPosition();
        if (group_position != null) {
            int num_supplies = world.getRandom().nextInt(MIN_CHICKENS_PER_GROUP,
                    MAX_CHICKENS_PER_GROUP + 1);
            var supply_positions = world.getUnitGrid().findGridTargets(group_position.x(), group_position.y(),
                    num_supplies, true);
            float spawn_x = UnitGrid.coordinateFromGrid(group_position.x());
            float spawn_y = UnitGrid.coordinateFromGrid(group_position.y());
            for (int i = 0; i < num_supplies; i++) {
                Target target = supply_positions[i];
                if (target == null) continue;
                int grid_x = target.getGridX();
                int grid_y = target.getGridY();
                float x = UnitGrid.coordinateFromGrid(grid_x);
                float y = UnitGrid.coordinateFromGrid(grid_y);
                RubberSupply supply = new RubberSupply(world, grid_x, grid_y, x, y, this, spawn_x, spawn_y);
                supplies.add(supply);
            }
            ((RubberSupplyManager) world.getSupplyManager(SupplyType.RUBBER)).newGroup();
        }
    }

    private @Nullable TreePlacement getGroupPosition() {
        List<TreePlacement> treePositions = world.getHeightMap().getTrees();
        if (treePositions.isEmpty()) {
            return null;
        }
        int startIndex = world.getRandom().nextInt(treePositions.size());
        int index = (startIndex + 1) % treePositions.size();
        while (index != startIndex) {
            TreePlacement placement = treePositions.get(index);
            Occupant occ = world.getUnitGrid().getOccupant(placement.x(), placement.y());
            if (occ instanceof TreeSupply)
                return placement;
            index = (index + 1) % treePositions.size();
        }
        return null;
    }

    public void remove(RubberSupply supply) {
        boolean in_list = supplies.remove(supply);
        assert in_list;
        if (supplies.isEmpty())
            ((RubberSupplyManager) world.getSupplyManager(SupplyType.RUBBER)).emptyGroup();
    }
}
