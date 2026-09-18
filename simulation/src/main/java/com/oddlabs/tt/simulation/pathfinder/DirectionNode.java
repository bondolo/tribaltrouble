package com.oddlabs.tt.simulation.pathfinder;

/**
 * Directional transition vector and inverse length between adjacent grid path nodes.
 *
 * @param invLength inverse length of the directional step
 * @param directionX delta X along the grid (-1, 0, or 1)
 * @param directionY delta Y along the grid (-1, 0, or 1)
 */
public record DirectionNode(float invLength, int directionX, int directionY) {
}
