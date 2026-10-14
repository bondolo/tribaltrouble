package com.oddlabs.tt.engine.resource;

import com.oddlabs.tt.engine.render.state.FogInfo;
import com.oddlabs.tt.simulation.landscape.Landscape;

/**
 * Configuration and resource details of a generated game world.
 * Generic type T represents the visual texture implementation type (e.g. Texture).
 *
 * @param landscape the simulation-owned landscape and topography
 * @param maps the diffuse and normal maps
 * @param detail the detail texture
 * @param detailNormal the detail normal map
 * @param fog_info the fog rendering configuration
 * @param <T> the texture type
 */
public record WorldInfo<T>(
                           Landscape landscape,
                           Maps<T> maps,
                           T detail,
                           T detailNormal,
                           FogInfo fog_info) {

    public record Maps<T>(T diffuse, T normal) {
    }
}
