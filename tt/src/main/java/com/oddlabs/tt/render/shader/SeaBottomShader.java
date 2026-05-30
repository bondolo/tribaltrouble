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
            out float v_height;

            void main() {
                vec4 worldPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
                gl_Position = u_projectionMatrix * worldPosition;

                v_texCoordDetail = in_Position.xy * u_detailScale;
                v_viewPosition = worldPosition.xyz;
                v_fogDist = length(worldPosition.xyz);
                v_height = in_Position.z;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ +
            GLOBAL_STATE_BLOCK +
            LIGHTING_CONSTANTS +
            FOG_FUNCTION +
            COLOR_SPACE_FUNCTIONS +
            """
                    uniform sampler2D u_texture1; // Detail texture
                    uniform vec4 u_baseColor;
                    uniform float u_detailScale;

                    in vec2 v_texCoordDetail;
                    in float v_fogDist;
                    in vec3 v_viewPosition;
                    in float v_height;

                    layout(location = 0) out vec4 out_FragColor;

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

                            // Match LandscapeShader's flat slope detail modulation underwater
                            float detailStrength = 0.15 * normalMapStrength;
                            float detailOffset = 1.0 - detailStrength * 0.5;
                            vec3 srgbColor = toSRGB(color.rgb);
                            srgbColor *= (detail.rgb * detailStrength + detailOffset);
                            color.rgb = toLinear(srgbColor);

                            // Perturb normal using detail map to match LandscapeShader's detail normal mapping
                            normal = normalize(normal + (detail.rgb - vec3(0.5)) * (0.08 * normalMapStrength));
                        }

                        vec3 viewDir = normalize(-v_viewPosition);
                        vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection, 0.0)).xyz);
                        float diff = dot(normal, lightDir) * 0.5 + 0.5;
                        diff = diff * diff;

                        // Hemispheric Ambient (flat Z=1.0 for horizontal sea bottom)
                        float skyWeight = 1.0;
                        vec3 ambient = mix(u_groundAmbient, u_globalAmbient, skyWeight);

                        float rim = 1.0 - max(dot(viewDir, normal), 0.0);
                        rim = smoothstep(0.8, 1.0, rim);
                        vec3 rimLight = rim * u_globalAmbient * 0.25;

                        float exposure = 1.1;
                        vec3 lightFactor = (ambient + diff * vec3(1.0) + rimLight) * exposure;
                        vec3 litColor = color.rgb * lightFactor;

                        float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                        out_FragColor = vec4(mix(u_fogColor.rgb, litColor, fogFactor), color.a);
                    }
                    """;

    public SeaBottomShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}
