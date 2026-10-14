package com.oddlabs.tt.simulation.landscape;

/**
 * Grid coordinate placement for a tree supply in the landscape.
 *
 * @param x grid column coordinate
 * @param y grid row coordinate
 */
public record TreePlacement(int x, int y) implements Placement {
}
