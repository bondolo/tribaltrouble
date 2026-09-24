package com.oddlabs.tt.engine.render.shader;

/**
 * Renders the sea bottom with detail texturing, lighting, normal mapping, and water caustics.
 */
public final class SeaBottomShader extends ShaderProgram implements FogShader, LitShader {

    private interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.Uniforms.MODEL_VIEW_MATRIX;
        String TEXTURE_1 = "u_texture1"; // Detail texture
        String TEXTURE_NORMAL = "u_textureNormal"; // Detail normal texture
        String BASE_COLOR = "u_baseColor";
        String DETAIL_SCALE = "u_detailScale";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec3 in_Position;

                    uniform mat4 u_modelViewMatrix;
                    uniform float u_detailScale;

                    out vec2 v_texCoordDetail;
                    out vec2 v_worldPos;
                    out float v_fogDist;
                    out vec3 v_viewPosition;
                    out float v_height;

                    void main() {
                        vec4 worldPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
                        gl_Position = u_projectionMatrix * worldPosition;

                        v_texCoordDetail = in_Position.xy * u_detailScale;
                        v_worldPos = in_Position.xy;
                        v_viewPosition = worldPosition.xyz;
                        v_fogDist = length(worldPosition.xyz);
                        v_height = in_Position.z;
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            FOG_FUNCTION +
            """
                    uniform sampler2D u_texture1; // Detail texture
                    uniform sampler2D u_textureNormal; // Detail normal texture
                    uniform vec4 u_baseColor;
                    uniform float u_detailScale;

                    in vec2 v_texCoordDetail;
                    in vec2 v_worldPos;
                    in float v_fogDist;
                    in vec3 v_viewPosition;
                    in float v_height;

                    layout(location = 0) out vec4 out_FragColor;

                    const float PI = 3.14159265358979;
                    const float GRAVITY = 9.81;

                    float getWaveHeight(vec2 worldPos) {
                        if (u_waveAmpSteep[0].x < 0.0001) {
                            return 0.0;
                        }
                        float waveZ = 0.0;
                        for (int i = 0; i < 3; i++) {
                            float waveLength = u_waveDirLength[i].z;
                            vec2 waveDir = u_waveDirLength[i].xy;
                            float waveAmplitude = u_waveAmpSteep[i].x;

                            float k = 2.0 * PI / waveLength;
                            float omega = sqrt(GRAVITY * k);
                            float phase = k * dot(waveDir, worldPos) - omega * u_waveTime;
                            waveZ += waveAmplitude * sin(phase);
                        }
                        return waveZ;
                    }

                    void main() {
                        vec4 color = u_baseColor;

                        vec3 viewNormal = normalize((u_viewMatrix * vec4(0.0, 0.0, 1.0, 0.0)).xyz);
                        vec3 normal = viewNormal;

                        float u_seaLevel = u_fogParams.w;
                        float depth = u_seaLevel - v_height;
                        float normalMapStrength;
                        if (depth < 0.0) {
                            float t = clamp((depth + 0.25) / 0.25, 0.0, 1.0);
                            normalMapStrength = mix(1.0, 0.50, t);
                        } else {
                            float t = clamp(depth / 1.0, 0.0, 1.0);
                            normalMapStrength = mix(0.50, 0.25, t);
                        }

                        if (u_detailScale > 0.0001) {
                            vec4 detail = texture(u_texture1, v_texCoordDetail);
                            vec4 detailNormal = texture(u_textureNormal, v_texCoordDetail);

                            float detailFade = clamp(detail.a / 0.15, 0.0, 1.0);
                            float grain = (detail.r - 0.5) * 0.35;
                            color.rgb = clamp(color.rgb * (1.0 + grain * detailFade), 0.0, 1.0);
                        }

                        vec3 viewDir = normalize(-v_viewPosition);
                        vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection.xyz, 0.0)).xyz);
                        vec3 halfDir = normalize(lightDir + viewDir);
                        // Add wet specular highlight to match the wet landscape
                        float spec = pow(max(dot(normal, halfDir), 0.0), 80.0);
                        vec3 specular = 0.05 * spec * vec3(1.0);

                        vec3 litColor = color.rgb + specular * 1.1;

                        // --- Underwater Caustics ---
                        float causticsTime = u_waveTime * 0.15;
                        vec2 uv1 = v_worldPos * 0.4 + vec2(causticsTime * 0.1, causticsTime * 0.07);
                        vec2 uv2 = v_worldPos * 0.3 - vec2(causticsTime * 0.08, causticsTime * 0.13);

                        float h1 = getWaveHeight(uv1);
                        float h2 = getWaveHeight(uv2);

                        // Stable refraction model with robust intensity
                        float c = 1.0 - abs(h1 - h2);
                        float caustic = pow(max(0.0, c), 20.0) * 0.15;

                        // Viewing angle dependency: fade out straight down
                        float vdn = dot(viewDir, normal);
                        float angleFade = smoothstep(0.1, 0.5, 1.0 - vdn);

                        float depthFade = clamp(1.0 - depth / 4.0, 0.0, 1.0);
                        vec3 causticColor = vec3(0.95, 0.98, 1.0) * caustic * depthFade * angleFade;
                        litColor += causticColor;

                        vec3 finalColor = applyFog(litColor, v_fogDist, gl_FragCoord.xy);
                        out_FragColor = vec4(finalColor, color.a);
                    }
                    """;

    public final int locModelViewMatrix;
    public final int locTexture1;
    public final int locTextureNormal;
    public final int locBaseColor;
    public final int locDetailScale;

    public SeaBottomShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
        locModelViewMatrix = getUniformLocation(Uniforms.MODEL_VIEW_MATRIX);
        locTexture1 = getUniformLocation(Uniforms.TEXTURE_1);
        locTextureNormal = getUniformLocation(Uniforms.TEXTURE_NORMAL);
        locBaseColor = getUniformLocation(Uniforms.BASE_COLOR);
        locDetailScale = getUniformLocation(Uniforms.DETAIL_SCALE);
    }
}
