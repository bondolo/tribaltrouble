package com.oddlabs.tt.engine.resource;

import com.oddlabs.tt.simulation.landscape.LandscapeData;
import org.jspecify.annotations.NonNull;

/**
 * Configuration and resource details of a generated game world.
 * Generic type T represents the visual texture implementation type (e.g. Texture).
 *
 * @param landscapeData the simulation-owned landscape and topography data
 * @param maps the diffuse and normal maps
 * @param detail the detail texture
 * @param detailNormal the detail normal map
 * @param fog_info the fog rendering configuration
 * @param blend_infos the terrain layer blending definitions
 * @param <T> the texture type
 */
public record WorldInfo<T>(
                           @NonNull LandscapeData landscapeData,
                           Maps<T> maps,
                           @NonNull T detail,
                           @NonNull T detailNormal,
                           @NonNull FogInfo fog_info,
                           @NonNull BlendInfo @NonNull [] blend_infos) {

    public record Maps<T>(T diffuse, T normal) {
    }
}
