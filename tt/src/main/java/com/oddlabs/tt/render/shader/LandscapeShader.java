package com.oddlabs.tt.render.shader;

public final class LandscapeShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String HEIGHT_MAP = "u_HeightMap";
        String DIFFUSE_MAP = "u_DiffuseMap";
        String DETAIL_MAP = "u_DetailMap";
        String WORLD_SIZE = "u_WorldSize";
        String DETAIL_SCALE = "u_DetailScale";
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String INSTANCE_PATCH_OFFSET = "in_InstancePatchOffset";
    }

    private static final String VERTEX_SHADER =
            """
                    #version 410 core
                    """ +
                    GLOBAL_STATE_BLOCK +
                    """
                            layout(location = 0) in vec2 in_Position;
                            layout(location = 4) in vec2 in_InstancePatchOffset;
                            
                            uniform float u_WorldSize;
                            uniform float u_DetailScale;
                            uniform sampler2D u_HeightMap;
                            
                            out vec2 v_texCoord0;
                            out vec2 v_texCoord1;
                            out float v_fogDist;
                            out vec3 v_viewPosition;
                            
                            void main() {
                                vec2 worldPos = in_InstancePatchOffset + in_Position;
                                // Add half-texel offset to align vertex-centered heightmap (1 grid unit = 2 meters)
                                vec2 uv = (worldPos + 1.0) / u_WorldSize;
                                float h = texture(u_HeightMap, uv).r;
                            
                                vec4 worldPosition4 = vec4(worldPos.x, worldPos.y, h, 1.0);
                                vec4 viewPosition = u_viewMatrix * worldPosition4;
                                gl_Position = u_projectionMatrix * viewPosition;
                            
                                v_texCoord0 = uv;
                                v_texCoord1 = worldPos * u_DetailScale;
                                v_fogDist = length(viewPosition.xyz);
                                v_viewPosition = viewPosition.xyz;
                            }
                            """;

    private static final String FRAGMENT_SHADER =
            """
                    #version 410 core
                    """ +
                    GLOBAL_STATE_BLOCK +
                    LIGHTING_CONSTANTS +
                    FOG_FUNCTION +
                    """
                            uniform sampler2D u_DiffuseMap;
                            uniform sampler2D u_DetailMap;
                            uniform sampler2D u_HeightMap;
                            
                            in vec2 v_texCoord0;
                            in vec2 v_texCoord1;
                            in float v_fogDist;
                            in vec3 v_viewPosition;
                            
                            layout(location = 0) out vec4 out_FragColor;
                            
                            void main() {
                                vec4 diffuseColor = texture(u_DiffuseMap, v_texCoord0);
                                vec4 detailColor = texture(u_DetailMap, v_texCoord1);
                            
                                // Subtly modulate diffuse color with detail noise (legacy parity range)
                                diffuseColor.rgb *= (detailColor.rgb * 0.4 + 0.8);
                            
                                // We rely on baked lighting from the LandscapeBaker for terrain.
                                // Dynamic lighting is disabled to ensure smooth legacy-style shading.
                                vec4 litColor = diffuseColor;
                            
                                // For fog, we still need a surface normal to blend correctly at the horizon
                                float h_plus_x = textureOffset(u_HeightMap, v_texCoord0, ivec2(1, 0)).r;
                                float h_minus_x = textureOffset(u_HeightMap, v_texCoord0, ivec2(-1, 0)).r;
                                float h_plus_y = textureOffset(u_HeightMap, v_texCoord0, ivec2(0, 1)).r;
                                float h_minus_y = textureOffset(u_HeightMap, v_texCoord0, ivec2(0, -1)).r;
                                vec3 worldNormal = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 64.0));
                            
                                float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                                vec3 finalColor = mix(u_fogColor.rgb, litColor.rgb, fogFactor);
                                out_FragColor = vec4(finalColor, litColor.a);
                            }
                            """;

    public LandscapeShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}
