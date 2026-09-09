package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingTemplate;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.ScanFilter;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Grid scanner filter identifying legal terrain positions for constructing buildings.
 */
public final class BuildingSiteScanFilter implements ScanFilter {
    private final UnitGrid unit_grid;
    private final BuildingTemplate template;
    private final int range;
    private final boolean singleTarget;
    private final List<LandscapeTarget> results;
    private @Nullable LandscapeTarget singleResult = null;

    public BuildingSiteScanFilter(UnitGrid unit_grid, BuildingTemplate template, int range) {
        this(unit_grid, template, range, true);
    }

    public BuildingSiteScanFilter(UnitGrid unit_grid, BuildingTemplate template, int range, boolean singleTarget) {
        this.unit_grid = unit_grid;
        this.template = template;
        this.range = range;
        this.singleTarget = singleTarget;
        this.results = singleTarget ? List.of() : new ArrayList<>();
    }

    @Override
    public int getMinRadius() {
        return 0;
    }

    @Override
    public int getMaxRadius() {
        return range;
    }

    @Override
    public boolean filter(int grid_x, int grid_y, @Nullable Occupant occ) {
        if (unit_grid.getHeightMap().canBuild(grid_x, grid_y, template.getPlacingSize()) &&
                Building.isPlacingLegal(unit_grid, template, grid_x, grid_y)) {
            LandscapeTarget target = new LandscapeTarget(grid_x, grid_y);
            if (singleTarget) {
                singleResult = target;
                return true;
            }
            results.add(target);
        }
        return false;
    }

    public Optional<LandscapeTarget> getSingleResult() {
        return Optional.ofNullable(singleResult);
    }

    public List<LandscapeTarget> getResults() {
        return results;
    }
}
