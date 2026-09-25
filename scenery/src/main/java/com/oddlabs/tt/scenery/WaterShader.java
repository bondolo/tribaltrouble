package com.oddlabs.tt.scenery;

import com.oddlabs.tt.engine.render.shader.FogShader;
import com.oddlabs.tt.engine.render.shader.LitShader;
import com.oddlabs.tt.engine.render.shader.Shader;
import com.oddlabs.tt.engine.render.shader.ShaderProgram;

/**
 * Renders water surfaces with dynamic wave animation, depth-based alpha blending, and sky reflections.
 */
final class WaterShader extends ShaderProgram implements FogShader, LitShader {

    private interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.Uniforms.MODEL_VIEW_MATRIX;
        String TEXTURE_0 = "u_texture0"; // Base water texture
        String TEXTURE_1 = "u_texture1"; // Detail Voronoi texture
        String ENABLE_DETAIL = "u_enableDetail";
        String CAMERA_POS = "u_cameraPos";
        String WATER_HEIGHT = "u_waterHeight";

        String HEIGHT_MAP = "u_HeightMap";
        String WORLD_SIZE = "u_WorldSize";
        String SKY_COLOR = "u_skyColor";
        String OCEAN_MASK = "u_oceanMask";

        // Sky and cloud reflection uniforms
        String CLOUD_TEXTURE_0 = "u_cloudTexture0";
        String CLOUD_TEXTURE_1 = "u_cloudTexture1";
        String INNER_OFFSET = "u_innerOffset";
        String OUTER_OFFSET = "u_outerOffset";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec3 in_Position;
                    layout(location = 4) in vec3 in_InstanceOffset;

                    uniform mat4 u_modelViewMatrix;
                    uniform float u_waterHeight;
                    uniform float u_WorldSize;
                    uniform sampler2D u_oceanMask;

                    out VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoord1;
                        vec2 texCoordHeightmap;
                        float fogDist;
                        vec3 worldPos;
                        vec3 normal;
                        float waveScale;
                        float waveDispZ;
                    } vs_out;

                    const float PI = 3.14159265358979;
                    const float GRAVITY = 9.81;

                    void addGerstnerWave(int i, vec2 baseXY, float waveScale, inout vec3 disp, inout vec3 normal) {
                        float waveLength = u_waveDirLength[i].z;
                        vec2 waveDir = u_waveDirLength[i].xy;
                        float waveAmplitude = u_waveAmpSteep[i].x;
                        float waveSteepness = u_waveAmpSteep[i].y;

                        float k = 2.0 * PI / waveLength;
                        float omega = sqrt(GRAVITY * k);
                        float phase = k * dot(waveDir, baseXY) - omega * u_waveTime;
                        float s = sin(phase);
                        float c = cos(phase);
                        float A = waveAmplitude * waveScale;

                        disp.x += waveSteepness * A * waveDir.x * c;
                        disp.y += waveSteepness * A * waveDir.y * c;
                        disp.z += A * s;

                        float WA = k * A;
                        normal.x -= WA * waveDir.x * c;
                        normal.y -= WA * waveDir.y * c;
                        normal.z -= waveSteepness * WA * s;
                    }

                    void main() {
                        vec2 baseXY = in_InstanceOffset.xy + in_Position.xy;
                        float baseZ = u_waterHeight + in_Position.z;

                        vec3 disp = vec3(0.0);
                        vec3 normal = vec3(0.0, 0.0, 1.0);

                        float waveScale = (in_InstanceOffset.z > 0.5) ? 1.0 : texture(u_oceanMask, (baseXY + 1.0) / u_WorldSize).r;
                        if (u_waveAmpSteep[0].x > 0.0001 && waveScale > 0.001) {
                            addGerstnerWave(0, baseXY, waveScale, disp, normal);
                            addGerstnerWave(1, baseXY, waveScale, disp, normal);
                            addGerstnerWave(2, baseXY, waveScale, disp, normal);
                        }

                        vec3 worldPos = vec3(baseXY + disp.xy, baseZ + disp.z);
                        vs_out.worldPos = worldPos;
                        vs_out.normal   = normalize(normal);
                        vs_out.waveScale = waveScale;
                        vs_out.waveDispZ = disp.z;

                        vec4 viewPosition = u_modelViewMatrix * vec4(worldPos, 1.0);
                        gl_Position = u_projectionMatrix * viewPosition;

                        float flowScale = waveScale;
                        vs_out.texCoord0 = (worldPos.xy * u_waterRepeatRate) + u_scrollOffsets.xy * flowScale;
                        vs_out.texCoord1 = (worldPos.xy * u_waterDetailRepeatRate) + u_scrollOffsets.zw * flowScale;
                        vs_out.texCoordHeightmap = (worldPos.xy + 1.0) / u_WorldSize;

                        vs_out.fogDist = length(viewPosition.xyz);
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            FOG_FUNCTION +
            """
                    uniform sampler2D u_texture0;
                    uniform sampler2D u_texture1;
                    uniform sampler2D u_HeightMap;
                    uniform bool u_enableDetail;
                    uniform vec3 u_cameraPos;
                    uniform float u_WorldSize;
                    uniform vec3 u_skyColor;

                    // Sky reflection uniforms
                    uniform sampler2D u_cloudTexture0;
                    uniform sampler2D u_cloudTexture1;
                    uniform vec2 u_innerOffset;
                    uniform vec2 u_outerOffset;

                    in VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoord1;
                        vec2 texCoordHeightmap;
                        float fogDist;
                        vec3 worldPos;
                        vec3 normal;
                        float waveScale;
                        float waveDispZ;
                    } fs_in;

                    layout(location = 0) out vec4 out_FragColor;
                    layout(location = 1) out vec4 out_MaskColor;

                    void main() {
                        vec4 baseColor = texture(u_texture0, fs_in.texCoord0);

                        // Gentle shoreline contact fade to prevent hard geometric clipping against terrain
                        vec2 closestPoint = clamp(fs_in.texCoordHeightmap.xy, 0.0, 1.0);
                        float terrainHeight = texture(u_HeightMap, closestPoint).r;
                        float distInMeters = distance(fs_in.texCoordHeightmap.xy, closestPoint) * u_WorldSize;
                        float depth = fs_in.worldPos.z - terrainHeight + distInMeters;
                        float edgeFade = smoothstep(0.0, 0.05, depth);
                        float finalAlpha = baseColor.a * edgeFade;

                        vec4 detail = texture(u_texture1, fs_in.texCoord1);

                        // Surface normal driven purely by physical Gerstner waves
                        vec3 normal = fs_in.normal;

                        vec3 lightDir = normalize(u_lightDirection.xyz);
                        vec3 viewDir = normalize(u_cameraPos - fs_in.worldPos);
                        vec3 halfDir = normalize(lightDir + viewDir);

                        // Base water color in linear HDR space with subtle optical variation
                        vec3 waterColor = pow(baseColor.rgb, vec3(2.2));
                        if (u_enableDetail) {
                            waterColor = mix(waterColor, waterColor * detail.rgb, detail.a * 0.15);
                        }

                        // Dynamic reflection vector sampling sky gradient and cloud decks
                        vec3 reflectDir = reflect(-viewDir, normal);
                        float horizonFactor = clamp(reflectDir.z, 0.0, 1.0);
                        vec3 reflectedSky = mix(u_fogColor.rgb, u_skyColor, horizonFactor);

                        if (reflectDir.z > 0.0) {
                            vec2 reflectUV0 = reflectDir.xy * 0.15 + u_innerOffset;
                            vec2 reflectUV1 = reflectDir.xy * 0.15 + u_outerOffset;
                            float cloud0 = texture(u_cloudTexture0, reflectUV0).r;
                            float cloud1 = texture(u_cloudTexture1, reflectUV1).r;
                            float cloudFactor = clamp(reflectDir.z * 1.5, 0.0, 1.0);
                            float cloudAlpha = (cloud0 * 0.20 + cloud1 * 0.12) * cloudFactor;
                            reflectedSky = mix(reflectedSky, vec3(1.0), cloudAlpha);
                        }

                        // Restrained Fresnel reflection (F0 = 0.02, capped at <= 0.25 to prevent milky washout)
                        float F0 = 0.02;
                        float F = F0 + (1.0 - F0) * pow(clamp(1.0 - max(dot(normal, viewDir), 0.0), 0.0, 1.0), 5.0);
                        float reflectionFactor = min(F * 0.40, 0.25);
                        vec3 finalRGB = mix(waterColor, reflectedSky, reflectionFactor);

                        // Directional sun specular glints under 70 degree sun
                        float specAngle = max(dot(normal, halfDir), 0.0);
                        float specular = pow(specAngle, 64.0) * 0.40;
                        finalRGB += vec3(specular);

                        vec3 finalColor = applyFog(finalRGB, fs_in.fogDist, gl_FragCoord.xy);
                        out_FragColor = vec4(finalColor, finalAlpha);

                        // Write water marker to mask buffer (alpha = 0.1)
                        // This identifies water pixels in the post-processing shader.
                        out_MaskColor = vec4(0.0, 0.0, 0.0, 0.1);
                    }
                    """;

    final int locModelViewMatrix;
    final int locTexture0;
    final int locTexture1;
    final int locEnableDetail;
    final int locCameraPos;
    final int locWaterHeight;
    final int locHeightMap;
    final int locWorldSize;
    final int locSkyColor;
    final int locOceanMask;
    final int locCloudTexture0;
    final int locCloudTexture1;
    final int locInnerOffset;
    final int locOuterOffset;

    WaterShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
        locModelViewMatrix = getUniformLocation(Uniforms.MODEL_VIEW_MATRIX);
        locTexture0 = getUniformLocation(Uniforms.TEXTURE_0);
        locTexture1 = getUniformLocation(Uniforms.TEXTURE_1);
        locEnableDetail = getUniformLocation(Uniforms.ENABLE_DETAIL);
        locCameraPos = getUniformLocation(Uniforms.CAMERA_POS);
        locWaterHeight = getUniformLocation(Uniforms.WATER_HEIGHT);
        locHeightMap = getUniformLocation(Uniforms.HEIGHT_MAP);
        locWorldSize = getUniformLocation(Uniforms.WORLD_SIZE);
        locSkyColor = getUniformLocation(Uniforms.SKY_COLOR);
        locOceanMask = getUniformLocation(Uniforms.OCEAN_MASK);
        locCloudTexture0 = getUniformLocation(Uniforms.CLOUD_TEXTURE_0);
        locCloudTexture1 = getUniformLocation(Uniforms.CLOUD_TEXTURE_1);
        locInnerOffset = getUniformLocation(Uniforms.INNER_OFFSET);
        locOuterOffset = getUniformLocation(Uniforms.OUTER_OFFSET);
    }
}
