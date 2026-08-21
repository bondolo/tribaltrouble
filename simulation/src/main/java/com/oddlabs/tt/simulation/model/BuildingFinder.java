package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.pathfinder.FinderFilter;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.Region;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.Nullable;

public final class BuildingFinder implements FinderFilter<Building> {
    private final Player owner;
    private final int abilities;

    public BuildingFinder(Player owner, int abilities) {
        this.owner = owner;
        this.abilities = abilities;
    }

    @Override
    public @Nullable Building getOccupantFromRegion(Region region, boolean one_region) {
        return region.getObjects(Building.class).stream()
                .filter(this::accept)
                .findFirst()
                .orElse(null);
    }

    @Override
    public @Nullable Building getBest() {
        return null;
    }

    private boolean accept(Building building) {
        return building.getOwner() == owner && building.getAbilities().hasAbilities(abilities);
    }

    @Override
    public boolean acceptOccupant(Occupant occ) {
        return occ instanceof Building building && accept(building);
    }
}
