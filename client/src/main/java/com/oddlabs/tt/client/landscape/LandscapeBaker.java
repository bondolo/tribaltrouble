package com.oddlabs.tt.client.landscape;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.engine.image.GLByteImage;
import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.FBO;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.state.DistanceFogInfo;
import com.oddlabs.tt.engine.render.state.SceneLighting;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.engine.vbo.QuadVBO;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.util.Color;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

import static com.oddlabs.tt.engine.util.GLUtils.checkGLError;

/**
 * Generates combined diffuse and normal maps for the landscape by baking layers.
 */
public final class LandscapeBaker {
    private static final Logger logger = Logger.getLogger(LandscapeBaker.class.getSimpleName());

    private static final int IDEAL_TEXELS_PER_DETAIL = 256;
    private static final float IDEAL_DETAIL_ALPHA = 0.15f;
    private static final float GRASS_ALPHA_POWER = 0.70f;

    private static final int DETAIL_FADEOUT_BASE_LEVEL = 2;
    private static final float DETAIL_FADEOUT_FACTOR = 0.75f;
    private static final float TEXTURE_SCALE = 1.0f / 64.0f;

    private LandscapeBaker() {
    }

    private static int getMaxTextureSize() {
        try {
            if (GL.getCapabilities() != null) {
                int max = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
                if (max > 0) {
                    return max;
                }
            }
        } catch (Throwable _) {
            // No OpenGL context is current
        }
        return 8192;
    }

    private static int clampTexelsPerGridUnit(int gridUnits, int requestedTexelsPerUnit) {
        int maxTextureSize = getMaxTextureSize();
        int texels = requestedTexelsPerUnit;
        while (gridUnits * texels > maxTextureSize && texels > 1) {
            texels >>= 1;
        }
        return texels;
    }

    private static Texture createDetail(GLImage detail_image, int base_level) {
        GLImage[] detail_mipmaps = detail_image.buildMipMaps(base_level,
                DETAIL_FADEOUT_FACTOR, true,
                false);
        return new Texture(detail_mipmaps, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static Texture createDetailNormal(GLImage detail_image) {
        GLImage[] detail_mipmaps = detail_image.buildMipMaps(10000, 1.0f, true, false);
        return new Texture(detail_mipmaps, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    public static WorldInfo<Texture> bakeWorld(Landscape landscape) {
        return bakeWorld(landscape, SceneLighting.CANONICAL);
    }

    public static WorldInfo<Texture> bakeWorld(Landscape landscape, SceneLighting lighting) {
        IslandConfig config = landscape.config();

        Instant time_before = Instant.now();
        int base_level = DETAIL_FADEOUT_BASE_LEVEL;
        int detail_mip_level = IDEAL_TEXELS_PER_DETAIL / LandscapeVisualGenerator.DETAIL_SIZE - 1;
        int detail_prefade_level = Math.max(detail_mip_level - base_level, 0);
        float detailAlpha = IDEAL_DETAIL_ALPHA * (float) Math.pow(DETAIL_FADEOUT_FACTOR,
                detail_prefade_level);

        LandscapeBakingData bakingData = LandscapeVisualGenerator.generate(
                landscape,
                lighting,
                detailAlpha,
                GRASS_ALPHA_POWER);
        BlendInfo[] blend_infos = bakingData.blendInfos();
        Texture detail = createDetail(new GLIntImage(bakingData.detail()),
                DETAIL_FADEOUT_BASE_LEVEL);
        Texture detailNormal = createDetailNormal(new GLIntImage(bakingData.detailNormal()));

        int grid_units = config.metersPerWorld() / HeightMap.METERS_PER_UNIT_GRID;
        int texels_per_grid_unit = clampTexelsPerGridUnit(grid_units, AppConfig.DEFAULT_TEXELS_PER_GRID_UNIT);
        int colormap_size = grid_units * texels_per_grid_unit;

        float textureScale = config.metersPerWorld() * TEXTURE_SCALE;
        int grid_width = config.metersPerWorld() / HeightMap.METERS_PER_UNIT_GRID;
        WorldInfo.Maps<Texture> maps;
        try (Texture heightMapTexture = new Texture(landscape.heightmap(), grid_width, grid_width,
                GL30.GL_R32F, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT)) {
            maps = bake(colormap_size, textureScale, heightMapTexture, config.metersPerWorld(), blend_infos);
        }
        Instant time_after = Instant.now();
        logger.fine(() -> "Landscape baked in " + Duration.between(time_before, time_after));

        return new WorldInfo<>(landscape, maps, detail, detailNormal,
                DistanceFogInfo.forTerrain(config.terrain(), config.metersPerWorld()));
    }

    private static Texture createAlphaMap(GLByteImage alpha_image) {
        return new Texture(new GLByteImage[]{alpha_image}, GL30.GL_R8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static Texture createStructureMap(GLIntImage structure_image) {
        return new Texture(new GLIntImage[]{structure_image}, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static Texture createNormalMap(GLIntImage normal_image) {
        return new Texture(new GLIntImage[]{normal_image}, GL11.GL_RGB, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static WorldInfo.Maps<Texture> bake(
            int colormapSize,
            float textureScale,
            Texture heightMap,
            float worldSize,
            BlendInfo[] blendInfos) {
        checkGLError("Before bake");
        Texture[] diffuse = new Texture[2];
        Texture[] normal = new Texture[2];

        for (int i = 0; i < 2; i++) {
            diffuse[i] = new Texture(colormapSize, colormapSize, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                    GL11.GL_REPEAT);
            checkGLError("After diffuse texture " + i);
            normal[i] = new Texture(colormapSize, colormapSize, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                    GL11.GL_REPEAT);
            checkGLError("After normal texture " + i);
        }

        boolean success = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Save current state
            IntBuffer viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int savedFBO = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int savedDrawBuffer = GL11.glGetInteger(GL30.GL_DRAW_BUFFER0);

            try (FBO fbo = new FBO(colormapSize, colormapSize);
                 LandscapeBakingShader shader = new LandscapeBakingShader();
                 QuadVBO quad = new QuadVBO()) {
                checkGLError("After resource creation");

                int current = 0;

                try (var _ = shader.use()) {
                    checkGLError("After shader use");

                    shader.setUniform(LandscapeBakingShader.Uniforms.BASE_DIFFUSE, 0);
                    shader.setUniform(LandscapeBakingShader.Uniforms.LAYER_DIFFUSE, 1);
                    shader.setUniform(LandscapeBakingShader.Uniforms.BASE_NORMAL, 2);
                    shader.setUniform(LandscapeBakingShader.Uniforms.LAYER_NORMAL, 3);
                    shader.setUniform(LandscapeBakingShader.Uniforms.ALPHA_MAP, 4);
                    shader.setUniform(LandscapeBakingShader.Uniforms.TEXTURE_SCALE, textureScale);
                    shader.setUniform(LandscapeBakingShader.Uniforms.WORLD_SIZE, worldSize);
                    shader.setUniform(LandscapeBakingShader.Uniforms.HEIGHT_MAP, 5);

                    IntBuffer drawBuffers = stack.mallocInt(2);
                    drawBuffers.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();

                    // Pre-clear both ping-pong pairs (diffuse to transparent, normal to neutral 0.5, 0.5, 1.0)
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo.getHandle());
                    GL11.glViewport(0, 0, colormapSize, colormapSize);
                    float[] normalClear = {0.5f, 0.5f, 1.0f, 0.0f};
                    for (int i = 0; i < 2; i++) {
                        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT0, diffuse[i]);
                        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT1, normal[i]);
                        GL30.glDrawBuffers(drawBuffers);
                        fbo.checkStatus();
                        GL11.glClearColor(0, 0, 0, 0);
                        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                        GL30.glClearBufferfv(GL30.GL_COLOR, 1, normalClear);
                    }

                    for (BlendInfo info : blendInfos) {
                        int src = current;
                        int dst = 1 - current;

                        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo.getHandle());
                        GL11.glViewport(0, 0, colormapSize, colormapSize);
                        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT0, diffuse[dst]);
                        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT1, normal[dst]);
                        GL30.glDrawBuffers(drawBuffers);
                        fbo.checkStatus();

                        try (Texture alphaMap = createAlphaMap(new GLByteImage(info.alphaChannel()))) {
                            GL13.glActiveTexture(GL13.GL_TEXTURE0);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse[src].getHandle());
                            GL13.glActiveTexture(GL13.GL_TEXTURE2);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, normal[src].getHandle());
                            GL13.glActiveTexture(GL13.GL_TEXTURE4);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, alphaMap.getHandle());
                            GL13.glActiveTexture(GL13.GL_TEXTURE5);
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, heightMap.getHandle());

                            switch (info) {
                                case StructureBlend(var struct, var norm, _, float alphaPower) -> {
                                    shader.setUniform(LandscapeBakingShader.Uniforms.MODE, LandscapeBakingShader.Modes.STRUCTURE_BLEND);
                                    shader.setUniform(LandscapeBakingShader.Uniforms.ALPHA_POWER, alphaPower);
                                    try (Texture structMap = createStructureMap(new GLIntImage(struct));
                                         Texture normMap = createNormalMap(new GLIntImage(norm))) {
                                        GL13.glActiveTexture(GL13.GL_TEXTURE1);
                                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, structMap.getHandle());
                                        GL13.glActiveTexture(GL13.GL_TEXTURE3);
                                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, normMap.getHandle());
                                        quad.render();
                                    }
                                }
                                case BlendLighting(_, Color color) -> {
                                    shader.setUniform(LandscapeBakingShader.Uniforms.MODE, LandscapeBakingShader.Modes.LIGHTING_BLEND);
                                    shader.setUniform(LandscapeBakingShader.Uniforms.ALPHA_POWER, 1.0f);
                                    Color std = new Color.Standard(color);
                                    shader.setUniform(LandscapeBakingShader.Uniforms.COLOR, std.r(), std.g(), std.b());
                                    quad.render();
                                }
                                case BlendOcclusion(_, Color color) -> {
                                    shader.setUniform(LandscapeBakingShader.Uniforms.MODE, LandscapeBakingShader.Modes.OCCLUSION_BLEND);
                                    shader.setUniform(LandscapeBakingShader.Uniforms.ALPHA_POWER, 1.0f);
                                    Color std = new Color.Standard(color);
                                    shader.setUniform(LandscapeBakingShader.Uniforms.COLOR, std.r(), std.g(), std.b());
                                    quad.render();
                                }
                            }
                        }

                        current = dst; // Flip
                    }
                }

                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse[current].getHandle());
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                    float maxAniso = GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
                    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                            maxAniso);
                }

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, normal[current].getHandle());
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                    float maxAniso = GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
                    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                            maxAniso);
                }

                // Detach textures from FBO before returning them
                // This prevents GL_INVALID_OPERATION when they are later bound as source textures (feedback loop).
                fbo.detachAll();

                // Delete the unused pair
                diffuse[1 - current].close();
                normal[1 - current].close();

                success = true;
                return new WorldInfo.Maps<>(diffuse[current], normal[current]);
            } finally {
                // Restore state
                GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFBO);
                GL11.glDrawBuffer(savedDrawBuffer);
            }
        } finally {
            if (!success) {
                diffuse[0].close();
                diffuse[1].close();
                normal[0].close();
                normal[1].close();
            }
        }
    }
}
