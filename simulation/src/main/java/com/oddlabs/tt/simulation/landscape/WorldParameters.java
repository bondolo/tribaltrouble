package com.oddlabs.tt.simulation.landscape;


import java.io.Serializable;

/**
 * Configuration parameters defining terrain speed, map code, and unit limits.
 *
 * @param initialGameSpeed the initial simulation game speed
 * @param mapCode the string code identifying the map/seed settings
 * @param initialUnitCount the initial unit count per player
 * @param maxUnitCount the maximum unit count per player
 */
public record WorldParameters(int initialGameSpeed, String mapCode,
                              int initialUnitCount, int maxUnitCount) implements Serializable {
}
