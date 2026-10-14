package com.oddlabs.tt.simulation.landscape;

/**
 * World coordinate spawn location for an initial unit.
 *
 * @param x world X coordinate in meters
 * @param y world Y coordinate in meters
 */
public record SpawnLocation(float x, float y) implements Location {
}
