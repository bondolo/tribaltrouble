package com.oddlabs.tt.simulation.behaviour;

import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.pathfinder.FinderFilter;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.Region;
import com.oddlabs.tt.simulation.player.Player;

import java.util.Optional;

/**
 * Filter strategy for locating candidate buildings owned by a specific player with matching abilities.
 */
final class BuildingFinder implements FinderFilter<Building> {
    private final Player owner;
    private final int abilities;

    BuildingFinder(Player owner, int abilities) {
        this.owner = owner;
        this.abilities = abilities;
    }

    @Override
    public Optional<Building> getOccupantFromRegion(Region region, boolean one_region) {
        return region.getObjects(Building.class).stream()
                .filter(this::accept)
                .findFirst();
    }

    @Override
    public Optional<Building> getBest() {
        return Optional.empty();
    }

    private boolean accept(Building building) {
        return building.getOwner() == owner && building.getAbilities().hasAbilities(abilities);
    }

    @Override
    public boolean acceptOccupant(Occupant occ) {
        return occ instanceof Building building && accept(building);
    }
}
