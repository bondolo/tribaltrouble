package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.FinderTrackerAlgorithm;
import org.jspecify.annotations.Nullable;

public final class TransferUnitController extends Controller {
    private final Unit unit;
    private @Nullable FinderTrackerAlgorithm<Building> building_tracker;

    public TransferUnitController(Unit unit) {
        super(1);
        this.unit = unit;
    }

    @Override
    public void decide() {
        if (building_tracker != null && building_tracker.getOccupant().isPresent() &&
                unit.isCloseEnough(0f, building_tracker.getOccupant().get())) {
            Building building = building_tracker.getOccupant().orElse(null);
            building.getUnitContainer().ifPresentOrElse(c -> {
                if (c.canEnter(unit))
                    c.enter(unit);
                else
                    unit.popController();
            }, unit::popController);
        } else if (!shouldGiveUp(0)) {
            building_tracker = new FinderTrackerAlgorithm<>(unit.getUnitGrid(),
                    new BuildingFinder(unit.getOwner(), Abilities.SUPPLY_CONTAINER));
            unit.setBehaviour(new WalkBehaviour(unit, building_tracker, false));
        } else {
            unit.popController();
        }
    }
}
