package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.engine.image.GLByteImage;
import com.oddlabs.tt.engine.image.GLImage;
import com.oddlabs.tt.engine.image.GLIntImage;
import com.oddlabs.tt.engine.render.shader.ShaderProgram;
import com.oddlabs.tt.engine.render.state.DistanceFogInfo;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.WorldInfo;
import com.oddlabs.tt.engine.vbo.QuadVBO;
import com.oddlabs.tt.procedural.BlendInfo;
import com.oddlabs.tt.procedural.BlendLighting;
import com.oddlabs.tt.procedural.BlendOcclusion;
import com.oddlabs.tt.procedural.GeneratedLandscapeData;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.LandscapeConfig;
import com.oddlabs.tt.procedural.StructureBlend;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.oddlabs.tt.engine.util.GLUtils.checkGLError;

/**
 * Generates combined diffuse and normal maps for the landscape by baking layers.
 */
public final class LandscapeBaker {
    private static final Logger logger = Logger.getLogger(LandscapeBaker.class.getSimpleName());

    private static final String VERTEX_SHADER = """
            #version 410 core
            layout(location = 0) in vec2 in_Position;
            layout(location = 1) in vec2 in_TexCoord;
            out vec2 v_texCoord;
            void main() {
                gl_Position = vec4(in_Position, 0.0, 1.0);
                v_texCoord = in_TexCoord;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            uniform sampler2D u_BaseDiffuse;
            uniform sampler2D u_LayerDiffuse;
            uniform sampler2D u_BaseNormal;
            uniform sampler2D u_LayerNormal;
            uniform sampler2D u_AlphaMap;
            uniform sampler2D u_HeightMap;
            uniform int u_Mode; // 0 = Blend, 1 = Light, 2 = Occlusion
            uniform float u_TextureScale;
            uniform float u_WorldSize;
            uniform vec3 u_Color;

            in vec2 v_texCoord;

            layout(location = 0) out vec4 out_Diffuse;
            layout(location = 1) out vec4 out_Normal;

            void main() {
                // Fetch base values (Hardware de-gamma from sRGB textures to Linear)
                vec4 baseDiff = texture(u_BaseDiffuse, v_texCoord);
                vec4 baseNorm = texture(u_BaseNormal, v_texCoord);
                float alpha = texture(u_AlphaMap, v_texCoord).r;

                if (u_Mode == 0) { // Structure Blend
                    // Fetch source textures with triplanar mapping for steep slopes
                    // Alignment: Add half-texel offset to match vertex-centered heightmap
                    vec2 hUV = (v_texCoord * u_WorldSize + 1.0) / u_WorldSize;
                    float h = texture(u_HeightMap, hUV).r;

                    // Compute world normal from heightmap (matching LandscapeShader logic)
                    float h_plus_x = textureOffset(u_HeightMap, hUV, ivec2(1, 0)).r;
                    float h_minus_x = textureOffset(u_HeightMap, hUV, ivec2(-1, 0)).r;
                    float h_plus_y = textureOffset(u_HeightMap, hUV, ivec2(0, 1)).r;
                    float h_minus_y = textureOffset(u_HeightMap, hUV, ivec2(0, -1)).r;
                    vec3 worldNormal = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 4.0));

                    // Triplanar weights (Soft power of 4.0 for smooth transitions)
                    vec3 blendWeights = pow(abs(worldNormal), vec3(4.0));
                    blendWeights /= (blendWeights.x + blendWeights.y + blendWeights.z);

                    // Use uniform texture scale for tiling
                    vec3 coord = vec3(v_texCoord, h / u_WorldSize) * u_TextureScale;

                    vec4 layerDiff = texture(u_LayerDiffuse, coord.xy) * blendWeights.z +
                                     texture(u_LayerDiffuse, coord.yz) * blendWeights.x +
                                     texture(u_LayerDiffuse, coord.xz) * blendWeights.y;

                    vec4 layerNorm;
                    // Decode input tangent normals [0, 1] -> [-1, 1]
                    vec3 nXY = texture(u_LayerNormal, coord.xy).rgb * 2.0 - 1.0;
                    vec3 nYZ = texture(u_LayerNormal, coord.yz).rgb * 2.0 - 1.0;
                    vec3 nXZ = texture(u_LayerNormal, coord.xz).rgb * 2.0 - 1.0;

                    // Swizzle side normals to world space based on face direction
                    // XY (Top): n.xyz
                    // YZ (Side X): (n.z * sign(worldNormal.x), n.x, n.y)
                    // XZ (Side Y): (n.x, n.z * sign(worldNormal.y), n.y)
                    vec3 worldN = normalize(nXY * blendWeights.z +
                                            vec3(nYZ.z * sign(worldNormal.x), nYZ.x, nYZ.y) * blendWeights.x +
                                            vec3(nXZ.x, nXZ.z * sign(worldNormal.y), nXZ.y) * blendWeights.y);

                    // Re-encode to [0, 1] using legacy formula (127-based)
                    layerNorm.rgb = (worldN * 127.0 + 128.0) / 255.0;
                    layerNorm.a = texture(u_LayerNormal, coord.xy).a * blendWeights.z +
                                  texture(u_LayerNormal, coord.yz).a * blendWeights.x +
                                  texture(u_LayerNormal, coord.xz).a * blendWeights.y;

                    // IMPORTANT: To match legacy visual look, we must blend in sRGB space.
                    // Samples are already de-gammaed by hardware to Linear.
                    vec3 srgbBase = pow(baseDiff.rgb, vec3(1.0 / 2.2));
                    vec3 srgbLayer = pow(layerDiff.rgb, vec3(1.0 / 2.2));
                    vec3 srgbMixed = mix(srgbBase, srgbLayer, alpha);

                    // Convert back to linear for the HDR output
                    out_Diffuse = vec4(pow(srgbMixed, vec3(2.2)), mix(baseDiff.a, layerDiff.a, alpha));
                    out_Normal = mix(baseNorm, layerNorm, alpha);
                } else if (u_Mode == 1) { // Lighting Blend
                    out_Diffuse = baseDiff + vec4(u_Color * alpha, 0.0);
                    out_Normal = baseNorm;
                } else { // Occlusion Blend (u_Mode == 2)
                    vec3 occluded = baseDiff.rgb * mix(vec3(1.0), u_Color, alpha);
                    out_Diffuse = vec4(occluded, baseDiff.a);
                    out_Normal = baseNorm;
                }
            }
            """;

    private static class BlendShader extends ShaderProgram {
        BlendShader() {
            super(VERTEX_SHADER, FRAGMENT_SHADER);
            // Layouts are defined in shader, no need for explicit bindFragDataLocation
            link();
        }
    }

    private final RenderContext renderContext;
    private final int colormapSize;
    private final float textureScale;
    private @Nullable Texture heightMap;
    private float worldSize;

    public LandscapeBaker(RenderContext renderContext, int colormapSize, float textureScale) {
        this.renderContext = renderContext;
        this.colormapSize = colormapSize;
        this.textureScale = textureScale;
    }

    public void setHeightMap(Texture heightMap, float worldSize) {
        this.heightMap = heightMap;
        this.worldSize = worldSize;
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
                LandscapeConfig.LANDSCAPE_DETAIL_FADEOUT_FACTOR, true,
                false);
        return new Texture(detail_mipmaps, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static Texture createDetailNormal(GLImage detail_image) {
        GLImage[] detail_mipmaps = detail_image.buildMipMaps(10000, 1.0f, true, false);
        return new Texture(detail_mipmaps, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    public static WorldInfo<Texture> bakeWorld(RenderContext renderContext, GeneratedLandscapeData landscapeData) {
        Landscape landscape = landscapeData.landscape();
        IslandConfig config = landscapeData.config();

        Instant time_before = Instant.now();
        BlendInfo[] blend_infos = landscape.getBlendInfos();
        Texture detail = createDetail(new GLIntImage(landscape.getDetail()), 1);
        Texture detailNormal = createDetailNormal(new GLIntImage(landscape.getDetailNormal()));

        int grid_units = config.metersPerWorld() / HeightMap.METERS_PER_UNIT_GRID;
        int texels_per_grid_unit = clampTexelsPerGridUnit(grid_units, AppConfig.DEFAULT_TEXELS_PER_GRID_UNIT);
        int colormap_size = grid_units * texels_per_grid_unit;

        float textureScale = config.metersPerWorld() * LandscapeConfig.LANDSCAPE_TEXTURE_SCALE;
        LandscapeBaker baker = new LandscapeBaker(renderContext, colormap_size, textureScale);

        int grid_width = config.metersPerWorld() / HeightMap.METERS_PER_UNIT_GRID;
        WorldInfo.Maps<Texture> maps;
        try (Texture heightMapTexture = new Texture(landscape.getHeight(), grid_width, grid_width,
                GL30.GL_R32F, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT)) {
            baker.setHeightMap(heightMapTexture, config.metersPerWorld());
            maps = baker.bake(blend_infos);
        }
        Instant time_after = Instant.now();
        logger.fine(() -> "Landscape baked in " + Duration.between(time_before, time_after));

        return new WorldInfo<>(landscapeData, maps, detail, detailNormal,
                DistanceFogInfo.forTerrain(config.terrain(), config.metersPerWorld()));
    }

    private static Texture createAlphaMap(GLByteImage alpha_image) {
        GLImage[] mipmaps = alpha_image.buildMipMaps(0, 1.0f, true, false);
        return new Texture(mipmaps, GL30.GL_R8, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT,
                GL11.GL_REPEAT);
    }

    private static Texture createStructureMap(GLIntImage structure_image) {
        return new Texture(new GLIntImage[]{structure_image}, GL21.GL_SRGB8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static Texture createNormalMap(GLIntImage normal_image) {
        return new Texture(new GLIntImage[]{normal_image}, GL11.GL_RGB, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    public WorldInfo.Maps<Texture> bake(BlendInfo[] blendInfos) {
        checkGLError("Before bake");
        Texture[] diffuse = new Texture[2];
        Texture[] normal = new Texture[2];
        List<Texture> tempTextures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            diffuse[i] = new Texture(colormapSize, colormapSize, GL21.GL_SRGB8_ALPHA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                    GL11.GL_REPEAT);
            checkGLError("After diffuse texture " + i);
            normal[i] = new Texture(colormapSize, colormapSize, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                    GL11.GL_REPEAT);
            checkGLError("After normal texture " + i);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Save current state
            IntBuffer viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int savedFBO = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int savedDrawBuffer = GL11.glGetInteger(GL30.GL_DRAW_BUFFER0);

            try (FBO fbo = new FBO(renderContext, colormapSize, colormapSize); BlendShader shader
                    = new BlendShader(); QuadVBO quad
                            = new QuadVBO()) {

                checkGLError("After resource creation");

                int current = 0;

                try (var _ = shader.use()) {
                    checkGLError("After shader use");
                    // Enable hardware sRGB support for diffuse attachment
                    boolean wasSrgb = GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB);
                    GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB);

                    shader.setUniform("u_BaseDiffuse", 0);
                    shader.setUniform("u_LayerDiffuse", 1);
                    shader.setUniform("u_BaseNormal", 2);
                    shader.setUniform("u_LayerNormal", 3);
                    shader.setUniform("u_AlphaMap", 4);
                    shader.setUniform("u_TextureScale", textureScale);
                    shader.setUniform("u_WorldSize", worldSize);
                    shader.setUniform("u_HeightMap", 5);

                    IntBuffer drawBuffers = stack.mallocInt(2);
                    drawBuffers.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();

                    boolean needsClear = true;
                    for (BlendInfo info : blendInfos) {
                        int src = current;
                        int dst = 1 - current;

                        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo.getHandle());
                        GL11.glViewport(0, 0, colormapSize, colormapSize);
                        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT0, diffuse[dst]);
                        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT1, normal[dst]);
                        GL30.glDrawBuffers(drawBuffers);
                        fbo.checkStatus();

                        if (needsClear) {
                            // Initialize diffuse to transparent and normal to neutral (0.5, 0.5, 1.0)
                            GL11.glClearColor(0, 0, 0, 0);
                            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT); // Attachment 0

                            // Attachment 1 (Normal) needs neutral normal
                            float[] normalClear = {0.5f, 0.5f, 1.0f, 0.0f};
                            GL30.glClearBufferfv(GL30.GL_COLOR, 1, normalClear);
                            needsClear = false;
                        }

                        Texture alphaMap = createAlphaMap(new GLByteImage(info.getAlphaChannel()));
                        tempTextures.add(alphaMap);

                        GL13.glActiveTexture(GL13.GL_TEXTURE0);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse[src].getHandle());
                        GL13.glActiveTexture(GL13.GL_TEXTURE2);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, normal[src].getHandle());
                        GL13.glActiveTexture(GL13.GL_TEXTURE4);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, alphaMap.getHandle());
                        GL13.glActiveTexture(GL13.GL_TEXTURE5);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, heightMap.getHandle());

                        switch (info) {
                            case StructureBlend sb -> {
                                shader.setUniform("u_Mode", 0);
                                Texture structMap = createStructureMap(new GLIntImage(sb.getStructureLayer()));
                                Texture normMap = createNormalMap(new GLIntImage(sb.getNormalLayer()));
                                tempTextures.add(structMap);
                                tempTextures.add(normMap);
                                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                                GL11.glBindTexture(GL11.GL_TEXTURE_2D, structMap.getHandle());
                                GL13.glActiveTexture(GL13.GL_TEXTURE3);
                                GL11.glBindTexture(GL11.GL_TEXTURE_2D, normMap.getHandle());
                            }
                            case BlendLighting bl -> {
                                shader.setUniform("u_Mode", 1);
                                shader.setUniformColor3("u_Color", bl.getColor());
                            }
                            case BlendOcclusion bo -> {
                                shader.setUniform("u_Mode", 2);
                                shader.setUniformColor3("u_Color", bo.getColor());
                            }
                            default -> {
                            }
                        }

                        quad.render();
                        current = dst; // Flip
                    }
                    if (!wasSrgb) GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
                }

                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse[current].getHandle());
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, normal[current].getHandle());
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);

                // Detach textures from FBO before returning them
                // This prevents GL_INVALID_OPERATION when they are later bound as source textures (feedback loop).
                fbo.detachAll();

                // Delete the unused pair
                diffuse[1 - current].close();
                normal[1 - current].close();

                return new WorldInfo.Maps<>(diffuse[current], normal[current]);
            } finally {
                // Clean up temporary blend textures
                for (Texture t : tempTextures) {
                    t.close();
                }
                // Restore state
                GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFBO);
                GL11.glDrawBuffer(savedDrawBuffer);
            }
        }
    }
}
