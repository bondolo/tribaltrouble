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

        String TIME = "u_time";
        String ENABLE_WAVES = "u_enableWaves";
        String WAVE_AMPLITUDE = "u_waveAmplitude";
        String WAVE_STEEPNESS = "u_waveSteepness";
        String WAVE_DIR = "u_waveDir";
        String WAVE_LENGTH = "u_waveLength";
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String INSTANCE_PATCH_OFFSET = "in_InstancePatchOffset";
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec2 in_Position;
            layout(location = 4) in vec3 in_InstancePatchOffset; // xy = offset, z = wave scale

            uniform float u_WorldSize;
            uniform float u_DetailScale;
            uniform sampler2D u_HeightMap;

            out vec2 v_texCoord0;
            out vec2 v_texCoordColormap;
            out vec2 v_texCoord1;
            out float v_fogDist;
            out vec3 v_viewPosition;
            out float v_height;
            out float v_waveScale;

            void main() {
                vec2 worldPos = in_InstancePatchOffset.xy + in_Position;
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
                v_height = h;
                v_waveScale = in_InstancePatchOffset.z;
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

                    // Wave uniforms for wetness calculation
                    uniform float u_time;
                    uniform bool u_enableWaves;
                    uniform float u_waveAmplitude[3];
                    uniform float u_waveSteepness[3];
                    uniform vec2 u_waveDir[3];
                    uniform float u_waveLength[3];
                    uniform float u_WorldSize;

                    in vec2 v_texCoord0;
                    in vec2 v_texCoordColormap;
                    in vec2 v_texCoord1;
                    in float v_fogDist;
                    in vec3 v_viewPosition;
                    in float v_height;
                    in float v_waveScale;

                    layout(location = 0) out vec4 out_FragColor;

                    const float PI = 3.14159265358979;
                    const float GRAVITY = 9.81;

                    float getWaveHeight(vec2 worldPos) {
                        if (!u_enableWaves) {
                            return 0.0;
                        }
                        float waveZ = 0.0;
                        for (int i = 0; i < 3; i++) {
                            float k = 2.0 * PI / u_waveLength[i];
                            float omega = sqrt(GRAVITY * k);
                            float phase = k * dot(u_waveDir[i], worldPos) - omega * u_time;
                            waveZ += u_waveAmplitude[i] * sin(phase);
                        }
                        return waveZ;
                    }

                    void main() {
                        vec4 diffuseColor = texture(u_DiffuseMap, v_texCoordColormap);
                        vec4 detailColor = texture(u_DetailMap, v_texCoord1);
                        vec4 normalMapVal = texture(u_NormalMap, v_texCoordColormap);

                        // Reconstruct world position and calculate dynamic wetness factor
                        vec2 worldPos = v_texCoordColormap * u_WorldSize;
                        float waveHeight = getWaveHeight(worldPos) * v_waveScale;
                        float u_seaLevel = u_fogParams.w;
                        float waterHeight = u_seaLevel + waveHeight;
                        float depth = waterHeight - v_height;
                        float wetness = clamp((depth + 0.05) / 0.20, 0.0, 1.0);

                        // Calculate underwater depth relative to sea level (heights above sea level have negative depth)
                        float depthStatic = u_seaLevel - v_height;
                        float normalMapStrength;
                        if (depthStatic < 0.0) {
                            float t = clamp((depthStatic + 0.25) / 0.25, 0.0, 1.0);
                            normalMapStrength = mix(1.0, 0.50, t);
                        } else {
                            float t = clamp(depthStatic / 1.0, 0.0, 1.0);
                            normalMapStrength = mix(0.50, 0.25, t);
                        }

                        // Calculate edge blend factor (ranges from 0.0 at the edge to 1.0 at 2% inside the world)
                        float distToEdgeX = min(v_texCoordColormap.x, 1.0 - v_texCoordColormap.x);
                        float distToEdgeY = min(v_texCoordColormap.y, 1.0 - v_texCoordColormap.y);
                        float distToEdge = min(distToEdgeX, distToEdgeY);
                        float edgeBlend = smoothstep(0.0, 0.02, distToEdge);

                        // Blend diffuse color to linear sea bottom color, and normal map alpha (specular intensity) to 0.0
                        diffuseColor.rgb = mix(u_SeaBottomColor, diffuseColor.rgb, edgeBlend);
                        normalMapVal.a = mix(0.0, normalMapVal.a, edgeBlend);

                        // Compute view-space normal from heightmap slope
                        float h_plus_x = textureOffset(u_HeightMap, v_texCoord0, ivec2(1, 0)).r;
                        float h_minus_x = textureOffset(u_HeightMap, v_texCoord0, ivec2(-1, 0)).r;
                        float h_plus_y = textureOffset(u_HeightMap, v_texCoord0, ivec2(0, 1)).r;
                        float h_minus_y = textureOffset(u_HeightMap, v_texCoord0, ivec2(0, -1)).r;
                        // Mathematically accurate normal (each texel spacing represents 2.0 meters, making 4.0 meters between plus and minus samples)
                        vec3 worldNormal = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 4.0));

                        // Smoothly blend worldNormal to flat (0,0,1) near the world edges to match the seabottom normal
                        worldNormal = normalize(mix(vec3(0.0, 0.0, 1.0), worldNormal, edgeBlend));

                        // Subtly modulate diffuse color with detail noise (legacy parity range in sRGB space)
                        // Steep slopes (high slope) get full contrast; flat terrain gets reduced contrast
                        float slope = 1.0 - worldNormal.z;
                        vec3 srgbDiffuse = pow(diffuseColor.rgb, vec3(1.0 / 2.2));
                        float detailStrength = mix(0.15, 0.4, slope) * normalMapStrength;
                        float detailOffset = 1.0 - detailStrength * 0.5;
                        srgbDiffuse *= (detailColor.rgb * detailStrength + detailOffset);
                        diffuseColor.rgb = pow(srgbDiffuse, vec3(2.2));

                        // Apply dynamic wetness darkening (wet surfaces scatter less light)
                        diffuseColor.rgb = mix(diffuseColor.rgb, diffuseColor.rgb * 0.55, wetness);

                        vec3 viewNormal = normalize((u_viewMatrix * vec4(worldNormal, 0.0)).xyz);

                        // Perturb using normal map
                        vec3 baseNormal = perturbNormal(viewNormal, normalize(v_viewPosition), v_texCoordColormap, normalMapVal.rgb);

                        // Reduce the normal map perturbation strength under water to smooth the underwater terrain
                        baseNormal = normalize(mix(viewNormal, baseNormal, normalMapStrength));

                        // Blend baseNormal back to the flat viewNormal near the boundary to eliminate TBN/derivative mismatches
                        baseNormal = normalize(mix(viewNormal, baseNormal, edgeBlend));

                        // Micro-detail normal perturbation from detail map color (adds tactile depth up close)
                        // Under water or in wet areas, the micro-detail normal is reduced
                        float detailNormalStrength = mix(0.08, 0.01, wetness) * normalMapStrength;
                        vec3 normal = normalize(baseNormal + (detailColor.rgb - vec3(0.5)) * detailNormalStrength);

                        // Dynamic specular (Blinn-Phong) & rim lighting
                        vec3 viewDir = normalize(-v_viewPosition);
                        vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection, 0.0)).xyz);
                        vec3 halfDir = normalize(lightDir + viewDir);

                        // For wet surfaces, blend to a sharper and more intense water-film specular highlight
                        float drySpecIntensity = normalMapVal.a * 0.05;
                        float wetSpecIntensity = 0.05;
                        float specExponent = mix(32.0, 80.0, wetness);
                        float specIntensity = mix(drySpecIntensity, wetSpecIntensity, wetness);
                        float spec = pow(max(dot(normal, halfDir), 0.0), specExponent);
                        vec3 specular = specIntensity * spec * vec3(1.0);

                        float rim = 1.0 - max(dot(viewDir, normal), 0.0);
                        rim = smoothstep(0.8, 1.0, rim);
                        vec3 rimLight = rim * u_globalAmbient * 0.05;

                        // Hemispheric Ambient (mix based on normal Z in World Space)
                        float skyWeight = clamp(worldNormal.z * 0.5 + 0.5, 0.0, 1.0);
                        vec3 ambient = mix(u_groundAmbient, u_globalAmbient, skyWeight);

                        // Wrap Lighting (Half-Lambert)
                        float diff = dot(normal, lightDir) * 0.5 + 0.5;
                        diff = diff * diff;

                        float exposure = 1.1;
                        vec3 lightFactor = (ambient + diff * vec3(1.0) + rimLight) * exposure;
                        vec3 litColor = diffuseColor.rgb * lightFactor + specular * exposure;

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
