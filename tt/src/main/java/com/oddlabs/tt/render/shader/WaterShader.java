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

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = FogShader.FOG_HEIGHT_FACTOR;
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String INSTANCE_OFFSET = "in_InstanceOffset";
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ +
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

                    out vec2 v_texCoord0;
                    out vec2 v_texCoord1;
                    out vec2 v_texCoordHeightmap;
                    out float v_fogDist;
                    out vec3 v_worldPos;

                    void main() {
                        vec3 worldPos = vec3(in_InstanceOffset + in_Position.xy, u_waterHeight + in_Position.z);
                        v_worldPos = worldPos;

                        vec4 viewPosition = u_modelViewMatrix * vec4(worldPos, 1.0);
                        gl_Position = u_projectionMatrix * viewPosition;

                        // Scale up the UVs significantly to reduce "blob" size.
                        float scaleFix = 4.0;

                        v_texCoord0 = (worldPos.xy * u_waterRepeatRate * scaleFix) + u_scrollOffset0;
                        v_texCoord1 = (worldPos.xy * u_waterRepeatRate * scaleFix * 1.3) + u_scrollOffset1;
                        v_texCoordHeightmap = (worldPos.xy + 1.0) / u_WorldSize;

                        v_fogDist = length(viewPosition.xyz);
                    }
                    """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ +
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

                    in vec2 v_texCoord0;
                    in vec2 v_texCoord1;
                    in vec2 v_texCoordHeightmap;
                    in float v_fogDist;
                    in vec3 v_worldPos;

                    layout(location = 0) out vec4 out_FragColor;

                    float getNoise(vec2 uv) {
                        return texture(u_texture0, uv).r;
                    }

                    vec2 getGradient(vec2 uv) {
                        float eps = 0.005;
                        float h = getNoise(uv);
                        float h_x = getNoise(uv + vec2(eps, 0.0));
                        float h_y = getNoise(uv + vec2(0.0, eps));
                        return vec2(h - h_x, h - h_y);
                    }

                    void main() {
                        vec4 baseColor = texture(u_texture0, v_texCoord0);

                        // Depth-based transparency: sample heightmap at closest point to determine water depth continuously
                        vec2 closestPoint = clamp(v_texCoordHeightmap.xy, 0.0, 1.0);
                        float terrainHeight = texture(u_HeightMap, closestPoint).r;
                        float distInMeters = distance(v_texCoordHeightmap.xy, closestPoint) * u_WorldSize;
                        float depth = v_worldPos.z - terrainHeight + distInMeters;
                        float depthFade = smoothstep(0.0, 1.0, clamp(depth / u_depthScale, 0.0, 1.0));
                        float finalAlpha = mix(u_minAlpha, u_maxAlpha, depthFade);

                        vec2 grad1 = getGradient(v_texCoord0);
                        vec2 grad2 = getGradient(v_texCoord1);
                        vec2 combinedGrad = (grad1 + grad2) * 0.5;

                        float normalStrength = 0.8;
                        vec3 normal = normalize(vec3(combinedGrad * normalStrength, 0.5));

                        vec3 lightDir = normalize(u_lightDirection);
                        vec3 viewDir = normalize(u_cameraPos - v_worldPos);
                        vec3 halfDir = normalize(lightDir + viewDir);

                        float specAngle = max(dot(normal, halfDir), 0.0);
                        float specular = pow(specAngle, 40.0);

                        float F0 = 0.02;
                        float F = F0 + (1.0 - F0) * pow(1.0 - max(dot(normal, viewDir), 0.0), 5.0);

                        // Calculate reflection vector in world space to dynamically sample sky gradient
                        vec3 reflectDir = reflect(-viewDir, normal);
                        float horizonFactor = clamp(reflectDir.z, 0.0, 1.0);
                        vec3 reflectionColor = mix(u_fogColor.rgb, u_skyColor, horizonFactor);
                        vec3 waterColor = baseColor.rgb * 0.7;

                        vec3 finalRGB = mix(waterColor, reflectionColor, F * 0.6);
                        finalRGB += vec3(specular) * 0.4;

                        if (u_enableDetail) {
                             vec4 detail = texture(u_texture1, v_texCoord0 * 2.0);
                             finalRGB = mix(finalRGB, detail.rgb, 0.05);
                        }

                        float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                        out_FragColor = vec4(mix(u_fogColor.rgb, finalRGB, fogFactor), finalAlpha);
                    }
                    """;

    public WaterShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}
