package com.oddlabs.tt.engine.render.shader;

/**
 * Renders water surfaces with dynamic wave animation, depth-based alpha blending, and sky reflections.
 */
public final class WaterShader extends ShaderProgram implements FogShader, LitShader {

    private interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.Uniforms.MODEL_VIEW_MATRIX;
        String TEXTURE_0 = "u_texture0"; // Base water texture
        String TEXTURE_1 = "u_texture1"; // Detail water texture
        String ENABLE_DETAIL = "u_enableDetail";
        String CAMERA_POS = "u_cameraPos";
        String WATER_HEIGHT = "u_waterHeight";

        String HEIGHT_MAP = "u_HeightMap";
        String WORLD_SIZE = "u_WorldSize";
        String DEPTH_SCALE = "u_depthScale";
        String MIN_ALPHA = "u_minAlpha";
        String MAX_ALPHA = "u_maxAlpha";
        String SKY_COLOR = "u_skyColor";

        // Fake sky reflection uniforms
        String CLOUD_TEXTURE_0 = "u_cloudTexture0";
        String CLOUD_TEXTURE_1 = "u_cloudTexture1";
        String INNER_OFFSET = "u_innerOffset";
        String OUTER_OFFSET = "u_outerOffset";
        String INNER_CLOUD_DENSITY = "u_innerCloudDensity";
        String OUTER_CLOUD_DENSITY = "u_outerCloudDensity";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec3 in_Position;
                    layout(location = 4) in vec3 in_InstanceOffset;

                    uniform mat4 u_modelViewMatrix;
                    uniform float u_waterHeight;
                    uniform float u_WorldSize;

                    out VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoord1;
                        vec2 texCoordHeightmap;
                        float fogDist;
                        vec3 worldPos;
                        vec3 normal;
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

                        // If amplitude is 0, waves are effectively disabled for that channel
                        if (u_waveAmpSteep[0].x > 0.0001 && in_InstanceOffset.z > 0.0) {
                            float distToEdgeX = min(baseXY.x, u_WorldSize - baseXY.x);
                            float distToEdgeY = min(baseXY.y, u_WorldSize - baseXY.y);
                            float distToEdge = min(distToEdgeX, distToEdgeY);

                            float waveScale;
                            if (u_waterHeight == 0.0) {
                                float edgeFade = clamp(-distToEdge / 16.0, 0.0, 1.0);
                                float distanceFade = clamp(in_Position.z / u_fogParams.w, 0.0, 1.0);
                                waveScale = edgeFade * distanceFade;
                            } else {
                                waveScale = clamp(distToEdge / 16.0, 0.0, 1.0);
                            }
                            waveScale *= in_InstanceOffset.z;

                            addGerstnerWave(0, baseXY, waveScale, disp, normal);
                            addGerstnerWave(1, baseXY, waveScale, disp, normal);
                            addGerstnerWave(2, baseXY, waveScale, disp, normal);
                        }

                        vec3 worldPos = vec3(baseXY + disp.xy, baseZ + disp.z);
                        vs_out.worldPos = worldPos;
                        vs_out.normal   = normalize(normal);

                        vec4 viewPosition = u_modelViewMatrix * vec4(worldPos, 1.0);
                        gl_Position = u_projectionMatrix * viewPosition;

                        float scaleFix = 4.0;
                        vs_out.texCoord0 = (worldPos.xy * u_waterRepeatRate * scaleFix) + u_scrollOffsets.xy;
                        vs_out.texCoord1 = (worldPos.xy * u_waterRepeatRate * scaleFix * 1.3) + u_scrollOffsets.zw;
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
                    uniform float u_depthScale;
                    uniform float u_minAlpha;
                    uniform float u_maxAlpha;
                    uniform float u_WorldSize;
                    uniform vec3 u_skyColor;

                    // Fake sky reflection uniforms
                    uniform sampler2D u_cloudTexture0;
                    uniform sampler2D u_cloudTexture1;
                    uniform vec2 u_innerOffset;
                    uniform vec2 u_outerOffset;
                    uniform float u_innerCloudDensity;
                    uniform float u_outerCloudDensity;

                    in VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoord1;
                        vec2 texCoordHeightmap;
                        float fogDist;
                        vec3 worldPos;
                        vec3 normal;
                    } fs_in;

                    layout(location = 0) out vec4 out_FragColor;
                    layout(location = 1) out vec4 out_MaskColor;

                    void main() {
                        vec4 baseColor = texture(u_texture0, fs_in.texCoord0);

                        // Depth-based transparency: sample heightmap at closest point to determine water depth continuously
                        vec2 closestPoint = clamp(fs_in.texCoordHeightmap.xy, 0.0, 1.0);
                        float terrainHeight = texture(u_HeightMap, closestPoint).r;
                        float distInMeters = distance(fs_in.texCoordHeightmap.xy, closestPoint) * u_WorldSize;
                        float depth = fs_in.worldPos.z - terrainHeight + distInMeters;
                        float depthFade = smoothstep(0.0, 1.0, clamp(depth / u_depthScale, 0.0, 1.0));
                        float finalAlpha = mix(u_minAlpha, u_maxAlpha, depthFade);

                        vec3 normal = normalize(fs_in.normal);
                        vec3 lightDir = normalize(u_lightDirection.xyz);
                        vec3 viewDir = normalize(u_cameraPos - fs_in.worldPos);
                        vec3 halfDir = normalize(lightDir + viewDir);

                        float specAngle = max(dot(normal, halfDir), 0.0);
                        float specular = pow(specAngle, 64.0);

                        float F0 = 0.02;
                        float F = F0 + (1.0 - F0) * pow(1.0 - max(dot(normal, viewDir), 0.0), 5.0);

                        // Calculate reflection vector in world space to dynamically sample sky gradient and clouds
                        vec3 reflectDir = reflect(-viewDir, normal);
                        float horizonFactor = clamp(reflectDir.z, 0.0, 1.0);
                        vec3 reflectionColor = mix(u_fogColor.rgb, u_skyColor, horizonFactor);

                        vec3 reflectedSky = reflectionColor;
                        if (reflectDir.z > 0.0) {
                            vec2 reflectUV0 = reflectDir.xy * 0.2 + u_innerOffset;
                            vec2 reflectUV1 = reflectDir.xy * 0.2 + u_outerOffset;

                            vec4 tex0 = texture(u_cloudTexture0, reflectUV0);
                            vec4 tex1 = texture(u_cloudTexture1, reflectUV1);

                            float exp0 = exp(-u_innerCloudDensity * 2.0);
                            float exp1 = exp(-u_outerCloudDensity * 2.0);

                            float cloud0 = pow(tex0.r, exp0);
                            float cloud1 = pow(tex1.r, exp1);

                            // Blend clouds subtly into the reflection color, fading out near the horizon
                            float cloudFactor = clamp(reflectDir.z * 1.5, 0.0, 1.0);
                            reflectedSky = mix(reflectedSky, u_skyColor, (cloud0 * 0.25 + cloud1 * 0.15) * cloudFactor);
                        }

                        vec3 waterColor = baseColor.rgb * 0.7;

                        vec3 finalRGB = mix(waterColor, reflectedSky, F * 0.6);
                        finalRGB += vec3(specular) * 0.5;

                        if (u_enableDetail) {
                             vec4 detail = texture(u_texture1, fs_in.texCoord0 * 2.0);
                             finalRGB = mix(finalRGB, detail.rgb, 0.05);
                        }

                        float fogFactor = calculateFogFactor(fs_in.fogDist, gl_FragCoord.xy);
                        out_FragColor = vec4(mix(u_fogColor.rgb, finalRGB, fogFactor), finalAlpha);

                        // Write water marker to mask buffer (alpha = 0.1)
                        // This identifies water pixels in the post-processing shader.
                        out_MaskColor = vec4(0.0, 0.0, 0.0, 0.1);
                    }
                    """;

    public final int locModelViewMatrix;
    public final int locTexture0;
    public final int locTexture1;
    public final int locEnableDetail;
    public final int locCameraPos;
    public final int locWaterHeight;
    public final int locHeightMap;
    public final int locWorldSize;
    public final int locDepthScale;
    public final int locMinAlpha;
    public final int locMaxAlpha;
    public final int locSkyColor;
    public final int locCloudTexture0;
    public final int locCloudTexture1;
    public final int locInnerOffset;
    public final int locOuterOffset;
    public final int locInnerCloudDensity;
    public final int locOuterCloudDensity;

    public WaterShader() {
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
        locDepthScale = getUniformLocation(Uniforms.DEPTH_SCALE);
        locMinAlpha = getUniformLocation(Uniforms.MIN_ALPHA);
        locMaxAlpha = getUniformLocation(Uniforms.MAX_ALPHA);
        locSkyColor = getUniformLocation(Uniforms.SKY_COLOR);
        locCloudTexture0 = getUniformLocation(Uniforms.CLOUD_TEXTURE_0);
        locCloudTexture1 = getUniformLocation(Uniforms.CLOUD_TEXTURE_1);
        locInnerOffset = getUniformLocation(Uniforms.INNER_OFFSET);
        locOuterOffset = getUniformLocation(Uniforms.OUTER_OFFSET);
        locInnerCloudDensity = getUniformLocation(Uniforms.INNER_CLOUD_DENSITY);
        locOuterCloudDensity = getUniformLocation(Uniforms.OUTER_CLOUD_DENSITY);
    }
}
