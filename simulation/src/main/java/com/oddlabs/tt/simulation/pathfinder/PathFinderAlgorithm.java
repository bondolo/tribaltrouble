package com.oddlabs.tt.simulation.pathfinder;

import java.util.Optional;

/**
 * Defines evaluation routines and cost heuristics for pathfinding graph searches.
 */
interface PathFinderAlgorithm {

    Optional<Node> touchNode(Node node);

    Optional<Node> getBestNode();

    int computeEstimatedCost(Node node);

    boolean touchNeighbour(Occupant occ);
}
