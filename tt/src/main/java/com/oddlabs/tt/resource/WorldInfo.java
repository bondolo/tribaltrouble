package com.oddlabs.tt.resource;

import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record WorldInfo(Landscape.@NonNull TerrainType terrain, int meters_per_world, float sea_level_meters,
                        int texels_per_colormap, int chunks_per_colormap,
                        @NonNull Texture @Nullable [] @NonNull [] colormaps, Maps maps, @NonNull Texture detail,
                        float @NonNull [] heightmap, @NonNull List<int @NonNull []> trees,
                        @NonNull List<int @NonNull[]> palm_trees, @NonNull List<int @NonNull []> rocks, @NonNull List<int @NonNull[]> iron,
                        float @NonNull [] @NonNull [] plants, boolean @NonNull [] @NonNull [] access_grid,
                        byte @NonNull [] @NonNull [] build_grid, float @NonNull [] @NonNull [] starting_locations,
                        @NonNull FogInfo fog_info,
                        @NonNull BlendInfo @NonNull [] blend_infos) {
    public record Maps(Texture diffuse, Texture normal) {
    }
}
