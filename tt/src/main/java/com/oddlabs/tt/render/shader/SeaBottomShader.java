package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering the sea bottom, including a detail texture, lighting, and fog.
 */
public final class SeaBottomShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String TEXTURE_1 = "u_texture1"; // Detail texture
        String BASE_COLOR = "u_baseColor";
        String DETAIL_SCALE = "u_detailScale";
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec3 in_Position;

            uniform mat4 u_modelViewMatrix;
            uniform float u_detailScale;

            out vec2 v_texCoordDetail;
            out float v_fogDist;
            out vec3 v_viewPosition;

            void main() {
                vec4 worldPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
                gl_Position = u_projectionMatrix * worldPosition;

                v_texCoordDetail = in_Position.xy * u_detailScale;
                v_viewPosition = worldPosition.xyz;
                v_fogDist = length(worldPosition.xyz);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ +
            GLOBAL_STATE_BLOCK +
            LIGHTING_CONSTANTS +
            FOG_FUNCTION +
            """
                    uniform sampler2D u_texture1; // Detail texture
                    uniform vec4 u_baseColor;
                    uniform float u_detailScale;

                    in vec2 v_texCoordDetail;
                    in float v_fogDist;
                    in vec3 v_viewPosition;

                    layout(location = 0) out vec4 out_FragColor;

                    void main() {
                        vec4 color = u_baseColor;

                        vec3 viewNormal = normalize((u_viewMatrix * vec4(0.0, 0.0, 1.0, 0.0)).xyz);
                        vec3 normal = viewNormal;

                        if (u_detailScale > 0.0001) {
                            vec4 detail = texture(u_texture1, v_texCoordDetail);
                            // Match LandscapeShader's subtle detail range [0.8, 1.2] in sRGB space
                            vec3 srgbColor = pow(color.rgb, vec3(1.0 / 2.2));
                            srgbColor *= (detail.rgb * 0.4 + 0.8);
                            color.rgb = pow(srgbColor, vec3(2.2));

                            // Perturb normal using detail map to match LandscapeShader's detail normal mapping
                            normal = normalize(normal + (detail.rgb - vec3(0.5)) * 0.08);
                        }

                        vec3 viewDir = normalize(-v_viewPosition);
                        float rim = 1.0 - max(dot(viewDir, normal), 0.0);
                        rim = smoothstep(0.8, 1.0, rim);
                        vec3 rimLight = rim * u_globalAmbient * 0.25;

                        float exposure = 1.1;
                        vec3 litColor = color.rgb * (1.0 + rimLight * exposure);

                        float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                        out_FragColor = vec4(mix(u_fogColor.rgb, litColor, fogFactor), color.a);
                    }
                    """;

    public SeaBottomShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}
