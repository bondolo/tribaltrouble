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
                    out float v_fogDist;

                    void main() {
                        vec4 worldPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
                        gl_Position = u_projectionMatrix * worldPosition;

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
                    in float v_fogDist;

                    layout(location = 0) out vec4 out_FragColor;

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
