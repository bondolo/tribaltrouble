package com.oddlabs.tt.simulation.pathfinder;

import java.util.Optional;

/**
 * Filter strategy for discovering and accepting occupants during spatial queries.
 */
public interface FinderFilter<O extends Occupant> {
    Optional<O> getOccupantFromRegion(Region region, boolean one_region);

    Optional<O> getBest();

    boolean acceptOccupant(Occupant occ);
}
