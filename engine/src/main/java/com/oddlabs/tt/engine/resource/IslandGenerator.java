package com.oddlabs.tt.engine.resource;

import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.engine.Globals;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.engine.render.LandscapeBaker;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.state.DistanceFogInfo;
import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.procedural.BlendInfo;
import com.oddlabs.tt.procedural.GeneratedLandscapeData;
import com.oddlabs.tt.procedural.Landscape;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;

/**
 * Generates landscape heights, terrain textures, and structures for game islands.
 */
public final class IslandGenerator implements WorldGenerator<WorldInfo<Texture>> {
    @Serial
    private static final long serialVersionUID = 1;

    private static final int IDEAL_TEXELS_PER_DETAIL = 256;
    private static final float IDEAL_DETAIL_ALPHA = .15f;

    private final @NonNull IslandConfig config;
    private final int grid_units;
    private final int texels_per_grid_unit;

    public IslandGenerator(@NonNull IslandConfig config, int texels_per_grid_unit) {
        this.config = config;
        this.grid_units = config.metersPerWorld() / HeightMap.METERS_PER_UNIT_GRID;
        this.texels_per_grid_unit = texels_per_grid_unit;
    }

    public @NonNull IslandConfig getConfig() {
        return config;
    }

    private static @NonNull Texture createDetail(@NonNull GLImage detail_image, int base_level) {
        GLImage[] detail_mipmaps = detail_image.buildMipMaps(base_level, Globals.LANDSCAPE_DETAIL_FADEOUT_FACTOR, true,
                false);
        return new Texture(detail_mipmaps, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static @NonNull Texture createDetailNormal(@NonNull GLImage detail_image) {
        GLImage[] detail_mipmaps = detail_image.buildMipMaps(10000, 1.0f, true, false);
        return new Texture(detail_mipmaps, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    @Override
    public int getMetersPerWorld() {
        return config.metersPerWorld();
    }

    @Override
    public @NonNull WorldInfo<Texture> generate(int num_players, int initial_unit_count, float random_start_pos) {
        int colormap_size = grid_units * texels_per_grid_unit;

        // Build landscape
        Instant time_before = Instant.now();
        int base_level = Globals.LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL;
        int detail_mip_level = IDEAL_TEXELS_PER_DETAIL / Globals.DETAIL_SIZE - 1;
        int detail_prefade_level = Math.max(detail_mip_level - base_level, 0);
        float detail_prefade = IDEAL_DETAIL_ALPHA * (float) Math.pow(Globals.LANDSCAPE_DETAIL_FADEOUT_FACTOR,
                detail_prefade_level);
        base_level -= detail_mip_level;
        base_level = Math.min(base_level, 1);
        Landscape landscape = new Landscape(num_players, config, detail_prefade, initial_unit_count,
                random_start_pos);
        Instant time_after = Instant.now();
        IO.println("Landscape created in " + Duration.between(time_before, time_after));
        time_before = Instant.now();
        BlendInfo[] blend_infos = landscape.getBlendInfos();
        Texture detail = createDetail(new GLIntImage(landscape.getDetail()), base_level);
        Texture detailNormal = createDetailNormal(new GLIntImage(landscape.getDetailNormal()));

        float textureScale = config.metersPerWorld() * Globals.LANDSCAPE_TEXTURE_SCALE;
        LandscapeBaker baker = new LandscapeBaker(colormap_size, textureScale);

        // Create temporary heightmap texture for baking
        int grid_width = config.metersPerWorld() / HeightMap.METERS_PER_UNIT_GRID;
        WorldInfo.Maps<Texture> maps;
        try (Texture heightMapTexture = new Texture(landscape.getHeight(), grid_width, grid_width,
                GL30.GL_R32F, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT)) {
            baker.setHeightMap(heightMapTexture, config.metersPerWorld());
            maps = baker.bake(blend_infos);
        }
        time_after = Instant.now();
        IO.println("Landscape baked in " + Duration.between(time_before, time_after));

        ProgressListener.progress();
        LandscapeData landscapeData = new GeneratedLandscapeData(config, landscape);
        return new WorldInfo<>(landscapeData, maps, detail, detailNormal,
                DistanceFogInfo.forTerrain(config.terrain(), config.metersPerWorld()));
    }
}
