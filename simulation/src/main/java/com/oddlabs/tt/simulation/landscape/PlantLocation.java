package com.oddlabs.tt.simulation.landscape;

/**
 * World coordinate placement for a cosmetic ground plant.
 *
 * @param x world X coordinate in meters
 * @param y world Y coordinate in meters
 */
public record PlantLocation(float x, float y) implements Location {
}
