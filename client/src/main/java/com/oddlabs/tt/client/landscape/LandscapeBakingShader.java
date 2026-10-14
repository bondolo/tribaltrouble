package com.oddlabs.tt.client.landscape;

import com.oddlabs.tt.engine.render.shader.ShaderProgram;

/**
 * Shader program for baking landscape diffuse and normal maps using layer blending.
 */
final class LandscapeBakingShader extends ShaderProgram {

    interface Uniforms {
        String BASE_DIFFUSE = "u_BaseDiffuse";
        String LAYER_DIFFUSE = "u_LayerDiffuse";
        String BASE_NORMAL = "u_BaseNormal";
        String LAYER_NORMAL = "u_LayerNormal";
        String ALPHA_MAP = "u_AlphaMap";
        String HEIGHT_MAP = "u_HeightMap";
        String MODE = "u_Mode";
        String TEXTURE_SCALE = "u_TextureScale";
        String WORLD_SIZE = "u_WorldSize";
        String ALPHA_POWER = "u_AlphaPower";
        String COLOR = "u_Color";
    }

    interface Modes {
        int STRUCTURE_BLEND = 0;
        int LIGHTING_BLEND = 1;
        int OCCLUSION_BLEND = 2;
    }

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
            uniform float u_AlphaPower;
            uniform vec3 u_Color;

            in vec2 v_texCoord;

            layout(location = 0) out vec4 out_Diffuse;
            layout(location = 1) out vec4 out_Normal;

            void main() {
                vec4 baseDiff = texture(u_BaseDiffuse, v_texCoord);
                vec4 baseNorm = texture(u_BaseNormal, v_texCoord);
                float alpha = texture(u_AlphaMap, v_texCoord).r;
                if (alpha > 0.0 && u_AlphaPower != 1.0) {
                    alpha = pow(alpha, u_AlphaPower);
                }

                if (u_Mode == 0) { // Structure Blend
                    vec2 coord = v_texCoord * u_TextureScale;
                    vec4 layerDiff = texture(u_LayerDiffuse, coord);
                    vec4 layerNorm = texture(u_LayerNormal, coord);

                    out_Diffuse = mix(baseDiff, layerDiff, alpha);
                    out_Normal = mix(baseNorm, layerNorm, alpha);
                } else if (u_Mode == 1) { // Lighting Blend
                    out_Diffuse = vec4(baseDiff.rgb * (vec3(1.0) + u_Color * alpha), baseDiff.a);
                    out_Normal = baseNorm;
                } else { // Occlusion Blend (u_Mode == 2)
                    out_Diffuse = vec4(baseDiff.rgb * mix(vec3(1.0), u_Color, alpha), baseDiff.a);
                    out_Normal = baseNorm;
                }
            }
            """;

    LandscapeBakingShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}
