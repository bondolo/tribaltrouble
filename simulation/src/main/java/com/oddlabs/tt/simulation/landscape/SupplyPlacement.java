package com.oddlabs.tt.simulation.landscape;

/**
 * Grid coordinate placement for a supply deposit (rock or iron) in the landscape.
 *
 * @param x grid column coordinate
 * @param y grid row coordinate
 */
public record SupplyPlacement(int x, int y) implements Placement {
}
