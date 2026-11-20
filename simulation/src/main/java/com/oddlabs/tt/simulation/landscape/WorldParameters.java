package com.oddlabs.tt.simulation.landscape;

import java.io.Serializable;

/**
 * Configuration parameters defining map code and unit limits.
 *
 * @param mapCode the string code identifying the map/seed settings
 * @param initialUnitCount the initial unit count per player
 * @param maxUnitCount the maximum unit count per player
 */
public record WorldParameters(String mapCode, int initialUnitCount, int maxUnitCount) implements Serializable {}
