package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering the dynamic 3D landscape with terrain texturing, normal mapping, and lighting.
 */
public final class LandscapeShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String HEIGHT_MAP = "u_HeightMap";
        String DIFFUSE_MAP = "u_DiffuseMap";
        String NORMAL_MAP = "u_NormalMap";
        String DETAIL_MAP = "u_DetailMap";
        String WORLD_SIZE = "u_WorldSize";
        String DETAIL_SCALE = "u_DetailScale";
        String SEA_BOTTOM_COLOR = "u_SeaBottomColor";
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String INSTANCE_PATCH_OFFSET = "in_InstancePatchOffset";
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec2 in_Position;
            layout(location = 4) in vec2 in_InstancePatchOffset;

            uniform float u_WorldSize;
            uniform float u_DetailScale;
            uniform sampler2D u_HeightMap;

            out vec2 v_texCoord0;
            out vec2 v_texCoordColormap;
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
                v_texCoordColormap = worldPos / u_WorldSize;
                v_texCoord1 = worldPos * u_DetailScale;
                v_fogDist = length(viewPosition.xyz);
                v_viewPosition = viewPosition.xyz;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ +
            GLOBAL_STATE_BLOCK +
            LIGHTING_CONSTANTS +
            FOG_FUNCTION +
            PERTURB_NORMAL_FUNC +
            """
                    uniform sampler2D u_DiffuseMap;
                    uniform sampler2D u_NormalMap;
                    uniform sampler2D u_DetailMap;
                    uniform sampler2D u_HeightMap;
                    uniform vec3 u_SeaBottomColor;

                    in vec2 v_texCoord0;
                    in vec2 v_texCoordColormap;
                    in vec2 v_texCoord1;
                    in float v_fogDist;
                    in vec3 v_viewPosition;

                    layout(location = 0) out vec4 out_FragColor;

                    void main() {
                        vec4 diffuseColor = texture(u_DiffuseMap, v_texCoordColormap);
                        vec4 detailColor = texture(u_DetailMap, v_texCoord1);
                        vec4 normalMapVal = texture(u_NormalMap, v_texCoordColormap);

                        // Calculate edge blend factor (ranges from 0.0 at the edge to 1.0 at 2% inside the world)
                        float distToEdgeX = min(v_texCoordColormap.x, 1.0 - v_texCoordColormap.x);
                        float distToEdgeY = min(v_texCoordColormap.y, 1.0 - v_texCoordColormap.y);
                        float distToEdge = min(distToEdgeX, distToEdgeY);
                        float edgeBlend = smoothstep(0.0, 0.02, distToEdge);

                        // Blend diffuse color to linear sea bottom color, and normal map alpha (specular intensity) to 0.0
                        diffuseColor.rgb = mix(u_SeaBottomColor, diffuseColor.rgb, edgeBlend);
                        normalMapVal.a = mix(0.0, normalMapVal.a, edgeBlend);

                        // Subtly modulate diffuse color with detail noise (legacy parity range in sRGB space)
                        vec3 srgbDiffuse = pow(diffuseColor.rgb, vec3(1.0 / 2.2));
                        srgbDiffuse *= (detailColor.rgb * 0.4 + 0.8);
                        diffuseColor.rgb = pow(srgbDiffuse, vec3(2.2));

                        // Compute view-space normal from heightmap slope
                        float h_plus_x = textureOffset(u_HeightMap, v_texCoord0, ivec2(1, 0)).r;
                        float h_minus_x = textureOffset(u_HeightMap, v_texCoord0, ivec2(-1, 0)).r;
                        float h_plus_y = textureOffset(u_HeightMap, v_texCoord0, ivec2(0, 1)).r;
                        float h_minus_y = textureOffset(u_HeightMap, v_texCoord0, ivec2(0, -1)).r;
                        // Mathematically accurate normal (each texel spacing represents 2.0 meters, making 4.0 meters between plus and minus samples)
                        vec3 worldNormal = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 4.0));

                        // Smoothly blend worldNormal to flat (0,0,1) near the world edges to match the seabottom normal
                        worldNormal = normalize(mix(vec3(0.0, 0.0, 1.0), worldNormal, edgeBlend));

                        vec3 viewNormal = normalize((u_viewMatrix * vec4(worldNormal, 0.0)).xyz);

                        // Perturb using normal map
                        vec3 baseNormal = perturbNormal(viewNormal, normalize(v_viewPosition), v_texCoordColormap, normalMapVal.rgb);
                        // Blend baseNormal back to the flat viewNormal near the boundary to eliminate TBN/derivative mismatches
                        baseNormal = normalize(mix(viewNormal, baseNormal, edgeBlend));

                        // Micro-detail normal perturbation from detail map color (adds tactile depth up close)
                        vec3 normal = normalize(baseNormal + (detailColor.rgb - vec3(0.5)) * 0.08);

                        // Dynamic specular (Blinn-Phong) & rim lighting
                        vec3 viewDir = normalize(-v_viewPosition);
                        vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection, 0.0)).xyz);
                        vec3 halfDir = normalize(lightDir + viewDir);
                        float spec = pow(max(dot(normal, halfDir), 0.0), 32.0);
                        vec3 specular = (normalMapVal.a * 0.15) * spec * vec3(1.0);

                        float rim = 1.0 - max(dot(viewDir, normal), 0.0);
                        rim = smoothstep(0.8, 1.0, rim);
                        vec3 rimLight = rim * u_globalAmbient * 0.25;

                        float exposure = 1.1;
                        vec3 litColor = diffuseColor.rgb * (1.0 + rimLight * exposure) + specular * exposure;

                        float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                        vec3 finalColor = mix(u_fogColor.rgb, litColor, fogFactor);
                        out_FragColor = vec4(finalColor, diffuseColor.a);
                    }
                    """;

    public LandscapeShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}
