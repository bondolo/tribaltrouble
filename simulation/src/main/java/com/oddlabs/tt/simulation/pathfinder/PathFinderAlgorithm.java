package com.oddlabs.tt.simulation.pathfinder;

import org.jspecify.annotations.Nullable;

/**
 * Defines evaluation routines and cost heuristics for pathfinding graph searches.
 */
interface PathFinderAlgorithm {

    @Nullable
    Node touchNode(Node node);

    @Nullable
    Node getBestNode();

    int computeEstimatedCost(Node node);

    boolean touchNeighbour(Occupant occ);
}
