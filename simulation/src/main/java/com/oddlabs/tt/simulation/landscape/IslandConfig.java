package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Terrain;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;

/**
 * Configuration parameters defining procedural island generation.
 *
 * @param terrain the landscape terrain type (e.g. tropical, winter, desert)
 * @param metersPerWorld the island size in meters
 * @param hills the hilliness factor
 * @param vegetation the vegetation density factor
 * @param supplies the resource supply deposit density factor
 * @param seed the random seed for procedural landscape generation
 */
public record IslandConfig(@NonNull Terrain terrain,
                           int metersPerWorld,
                           float hills,
                           float vegetation,
                           float supplies,
                           int seed) implements Serializable {
}
