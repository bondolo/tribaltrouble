package com.oddlabs.tt.scenery;

import com.oddlabs.tt.engine.render.shader.FogShader;
import com.oddlabs.tt.engine.render.shader.Shader;
import com.oddlabs.tt.engine.render.shader.ShaderProgram;

/**
 * Renders the ocean floor scenery with detail texturing and atmospheric fog.
 */
final class SeaBottomShader extends ShaderProgram implements FogShader {

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

                    void main() {
                        vec4 worldPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
                        gl_Position = u_projectionMatrix * worldPosition;

                        v_worldPos = in_Position.xy;
                        v_texCoordDetail = in_Position.xy * u_detailScale;
                        v_fogDist = length(worldPosition.xyz);
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

                        if (u_detailScale > 0.0001) {
                            vec4 detail = texture(u_texture1, v_texCoordDetail);
                            vec4 detailNorm = texture(u_textureNormal, v_texCoordDetail);

                            // Decal blending in display/sRGB space matching legacy GL_DECAL contract.
                            vec3 srgbColor = pow(color.rgb, vec3(1.0 / 2.2));
                            vec3 srgbMixed = mix(srgbColor, detail.rgb, detail.a);
                            color.rgb = pow(srgbMixed, vec3(2.2));

                            // Subtle micro-normal modulation under directional sun in world space
                            vec3 lightDir = normalize(u_lightDirection.xyz);
                            vec3 N = normalize(vec3((detailNorm.xy - 0.5) * 2.0, detailNorm.z));
                            float diffuseMod = 1.0 + 0.30 * (dot(N, lightDir) - lightDir.z);
                            color.rgb *= mix(1.0, diffuseMod, detailNorm.a * 0.4);
                        }

                        // --- Underwater Caustics ---
                        float u_seaLevel = u_fogParams.w;
                        float depthStatic = u_seaLevel;
                        if (depthStatic > 0.0 && u_waveAmpSteep[0].x > 0.0001) {
                            float causticsTime = u_waveTime * 0.05;
                            vec2 uv1 = v_worldPos * 0.15 + vec2(causticsTime * 0.08, causticsTime * 0.05);
                            vec2 uv2 = v_worldPos * 0.12 - vec2(causticsTime * 0.06, causticsTime * 0.10);

                            float h1 = getWaveHeight(uv1);
                            float h2 = getWaveHeight(uv2);

                            float c = 1.0 - abs(h1 - h2);
                            float caustic = pow(max(0.0, c), 16.0);

                            float depthFade = clamp(1.0 - depthStatic / 8.0, 0.0, 1.0);
                            float causticFactor = caustic * depthFade;
                            color.rgb *= (1.0 + causticFactor * 0.35);
                        }

                        vec3 finalColor = applyFog(color.rgb, v_fogDist, gl_FragCoord.xy);
                        out_FragColor = vec4(finalColor, color.a);
                    }
                    """;

    final int locModelViewMatrix;
    final int locTexture1;
    final int locTextureNormal;
    final int locBaseColor;
    final int locDetailScale;

    SeaBottomShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
        locModelViewMatrix = getUniformLocation(Uniforms.MODEL_VIEW_MATRIX);
        locTexture1 = getUniformLocation(Uniforms.TEXTURE_1);
        locTextureNormal = getUniformLocation(Uniforms.TEXTURE_NORMAL);
        locBaseColor = getUniformLocation(Uniforms.BASE_COLOR);
        locDetailScale = getUniformLocation(Uniforms.DETAIL_SCALE);
    }
}
