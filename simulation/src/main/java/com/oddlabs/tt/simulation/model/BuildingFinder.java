package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.pathfinder.FinderFilter;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.Region;
import com.oddlabs.tt.simulation.player.Player;
import java.util.Optional;
import java.util.function.Predicate;

/// Filter strategy for locating candidate buildings owned by a specific player matching a predicate.
final class BuildingFinder implements FinderFilter<Building> {
  private final Player owner;
  private final Predicate<Building> filter;

  BuildingFinder(Player owner, Predicate<Building> filter) {
    this.owner = owner;
    this.filter = filter;
  }

  @Override
  public Optional<Building> getOccupantFromRegion(Region region, boolean one_region) {
    for (Building building : region.getBuildings()) {
      if (accept(building)) {
        return Optional.of(building);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Building> getBest() {
    return Optional.empty();
  }

  private boolean accept(Building building) {
    return building.getOwner() == owner && filter.test(building);
  }

  @Override
  public boolean acceptOccupant(Occupant occ) {
    return occ instanceof Building building && accept(building);
  }
}
