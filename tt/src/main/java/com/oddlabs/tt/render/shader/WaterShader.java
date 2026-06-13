package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering water surfaces.
 */
public final class WaterShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String TEXTURE_0 = "u_texture0"; // Base water texture
        String TEXTURE_1 = "u_texture1"; // Detail water texture
        String WATER_REPEAT_RATE = "u_waterRepeatRate";
        String WATER_DETAIL_REPEAT_RATE = "u_waterDetailRepeatRate";
        String ENABLE_DETAIL = "u_enableDetail";
        String SCROLL_OFFSET_0 = "u_scrollOffset0";
        String SCROLL_OFFSET_1 = "u_scrollOffset1";
        String CAMERA_POS = "u_cameraPos";
        String WATER_HEIGHT = "u_waterHeight";

        String HEIGHT_MAP = "u_HeightMap";
        String WORLD_SIZE = "u_WorldSize";
        String DEPTH_SCALE = "u_depthScale";
        String MIN_ALPHA = "u_minAlpha";
        String MAX_ALPHA = "u_maxAlpha";
        String SKY_COLOR = "u_skyColor";

        // Gerstner wave uniforms
        String TIME = "u_time";
        String ENABLE_WAVES = "u_enableWaves";
        String WAVE_AMPLITUDE = "u_waveAmplitude";
        String WAVE_STEEPNESS = "u_waveSteepness";
        String WAVE_DIR = "u_waveDir";
        String WAVE_LENGTH = "u_waveLength";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = FogShader.FOG_HEIGHT_FACTOR;

        // Fake sky reflection uniforms
        String CLOUD_TEXTURE_0 = "u_cloudTexture0";
        String CLOUD_TEXTURE_1 = "u_cloudTexture1";
        String INNER_OFFSET = "u_innerOffset";
        String OUTER_OFFSET = "u_outerOffset";
        String INNER_CLOUD_DENSITY = "u_innerCloudDensity";
        String OUTER_CLOUD_DENSITY = "u_outerCloudDensity";
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String INSTANCE_OFFSET = "in_InstanceOffset";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec3 in_Position;
                    layout(location = 4) in vec2 in_InstanceOffset;

                    uniform mat4 u_modelViewMatrix;
                    uniform float u_waterRepeatRate;
                    uniform float u_waterDetailRepeatRate;
                    uniform vec2 u_scrollOffset0;
                    uniform vec2 u_scrollOffset1;
                    uniform float u_waterHeight;
                    uniform float u_WorldSize;

                    uniform float u_time;
                    uniform bool u_enableWaves;
                    uniform float u_waveAmplitude[3];
                    uniform float u_waveSteepness[3];
                    uniform vec2  u_waveDir[3];
                    uniform float u_waveLength[3];

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
                        float k = 2.0 * PI / u_waveLength[i];
                        float omega = sqrt(GRAVITY * k);
                        float phase = k * dot(u_waveDir[i], baseXY) - omega * u_time;
                        float s = sin(phase);
                        float c = cos(phase);
                        float A = u_waveAmplitude[i] * waveScale;
                        float Q = u_waveSteepness[i];

                        disp.x += Q * A * u_waveDir[i].x * c;
                        disp.y += Q * A * u_waveDir[i].y * c;
                        disp.z += A * s;

                        float WA = k * A;
                        normal.x -= WA * u_waveDir[i].x * c;
                        normal.y -= WA * u_waveDir[i].y * c;
                        normal.z -= Q * WA * s;
                    }

                    void main() {
                        vec2 baseXY = in_InstanceOffset + in_Position.xy;
                        float baseZ = u_waterHeight + in_Position.z;

                        vec3 disp = vec3(0.0);
                        vec3 normal = vec3(0.0, 0.0, 1.0);

                        if (u_enableWaves) {
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
                        vs_out.texCoord0 = (worldPos.xy * u_waterRepeatRate * scaleFix) + u_scrollOffset0;
                        vs_out.texCoord1 = (worldPos.xy * u_waterRepeatRate * scaleFix * 1.3) + u_scrollOffset1;
                        vs_out.texCoordHeightmap = (worldPos.xy + 1.0) / u_WorldSize;

                        vs_out.fogDist = length(viewPosition.xyz);
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            LIGHTING_CONSTANTS +
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
                        vec3 lightDir = normalize(u_lightDirection);
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
                    }
                    """;

    public WaterShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}
