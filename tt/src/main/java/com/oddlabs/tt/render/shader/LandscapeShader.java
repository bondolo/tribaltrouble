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
        String DETAIL_NORMAL_MAP = "u_DetailNormalMap";
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

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec2 in_Position;
                    layout(location = 4) in vec3 in_InstancePatchOffset; // xy = offset, z = wave scale

                    uniform float u_WorldSize;
                    uniform float u_DetailScale;
                    uniform sampler2D u_HeightMap;

                    out VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoordColormap;
                        vec2 texCoord1;
                        float fogDist;
                        vec3 viewPosition;
                        float height;
                        float waveScale;
                    } vs_out;

                    void main() {
                        vec2 worldPos = in_InstancePatchOffset.xy + in_Position;
                        // Add half-texel offset to align vertex-centered heightmap (1 grid unit = 2 meters)
                        vec2 uv = (worldPos + 1.0) / u_WorldSize;
                        float h = texture(u_HeightMap, uv).r;

                        vec4 worldPosition4 = vec4(worldPos.x, worldPos.y, h, 1.0);
                        vec4 viewPosition = u_viewMatrix * worldPosition4;
                        gl_Position = u_projectionMatrix * viewPosition;

                        vs_out.texCoord0 = uv;
                        vs_out.texCoordColormap = worldPos / u_WorldSize;
                        vs_out.texCoord1 = worldPos * u_DetailScale;
                        vs_out.fogDist = length(viewPosition.xyz);
                        vs_out.viewPosition = viewPosition.xyz;
                        vs_out.height = h;
                        vs_out.waveScale = in_InstancePatchOffset.z;
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            LIGHTING_CONSTANTS +
            FOG_FUNCTION +
            """
                    uniform sampler2D u_DiffuseMap;
                    uniform sampler2D u_NormalMap;
                    uniform sampler2D u_DetailMap;
                    uniform sampler2D u_DetailNormalMap;
                    uniform sampler2D u_HeightMap;
                    uniform vec3 u_SeaBottomColor;
                    uniform float u_WorldSize;
                    uniform float u_DetailScale;

                    // Wave uniforms for wetness calculation
                    uniform float u_time;
                    uniform bool u_enableWaves;
                    uniform float u_waveAmplitude[3];
                    uniform float u_waveSteepness[3];
                    uniform vec2 u_waveDir[3];
                    uniform float u_waveLength[3];
                    in VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoordColormap;
                        vec2 texCoord1;
                        float fogDist;
                        vec3 viewPosition;
                        float height;
                        float waveScale;
                    } fs_in;

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
                        vec4 diffuseColor = texture(u_DiffuseMap, fs_in.texCoordColormap);
                        vec4 normalMapVal = texture(u_NormalMap, fs_in.texCoordColormap);
                        vec4 detailColor;
                        vec4 detailNormalColor;

                        // Reconstruct world position and calculate dynamic wetness factor
                        vec2 worldPos = fs_in.texCoordColormap * u_WorldSize;
                        float waveHeight = getWaveHeight(worldPos) * fs_in.waveScale;
                        float u_seaLevel = u_fogParams.w;
                        float waterHeight = u_seaLevel + waveHeight;
                        float depth = waterHeight - fs_in.height;
                        float wetness = clamp((depth + 0.05) / 0.20, 0.0, 1.0);

                        // Calculate underwater depth relative to sea level (heights above sea level have negative depth)
                        float depthStatic = u_seaLevel - fs_in.height;
                        float normalMapStrength;
                        if (depthStatic < 0.0) {
                            float t = clamp((depthStatic + 0.25) / 0.25, 0.0, 1.0);
                            normalMapStrength = mix(1.0, 0.50, t);
                        } else {
                            float t = clamp(depthStatic / 1.0, 0.0, 1.0);
                            normalMapStrength = mix(0.50, 0.25, t);
                        }

                        // Calculate edge blend factor (ranges from 0.0 at the edge to 1.0 at 2% inside the world)
                        float distToEdgeX = min(fs_in.texCoordColormap.x, 1.0 - fs_in.texCoordColormap.x);
                        float distToEdgeY = min(fs_in.texCoordColormap.y, 1.0 - fs_in.texCoordColormap.y);
                        float distToEdge = min(distToEdgeX, distToEdgeY);
                        float edgeBlend = smoothstep(0.0, 0.02, distToEdge);

                        // Blend diffuse color to linear sea bottom color, and normal map alpha (specular intensity) to 0.0
                        diffuseColor.rgb = mix(u_SeaBottomColor, diffuseColor.rgb, edgeBlend);
                        normalMapVal.a = mix(0.0, normalMapVal.a, edgeBlend);

                        // Compute view-space normal from heightmap slope
                        float h_plus_x = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(1, 0)).r;
                        float h_minus_x = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(-1, 0)).r;
                        float h_plus_y = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(0, 1)).r;
                        float h_minus_y = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(0, -1)).r;

                        // Calculate mathematically accurate normal for triplanar mapping and specular
                        vec3 worldNormalGeom = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 4.0));
                        vec3 worldNormal = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 64.0));

                        // Smoothly blend worldNormal to flat (0,0,1) near the world edges to match the seabottom normal
                        worldNormalGeom = normalize(mix(vec3(0.0, 0.0, 1.0), worldNormalGeom, edgeBlend));
                        worldNormal = normalize(mix(vec3(0.0, 0.0, 1.0), worldNormal, edgeBlend));

                        // Decode world-space normal from the baked colormap
                        vec3 bakedWorldNormal = normalMapVal.rgb * (255.0/127.0) - (128.0/127.0);
                        vec3 baseNormal = normalize((u_viewMatrix * vec4(bakedWorldNormal, 0.0)).xyz);

                        // Sample detail map and detail normal map using triplanar mapping for steep slopes (cliffs)
                        // This handles high-frequency noise tiling which is too dense to bake into the colormap.
                        vec3 blendWeights = pow(abs(worldNormalGeom), vec3(8.0));
                        blendWeights /= (blendWeights.x + blendWeights.y + blendWeights.z);

                        if (worldNormalGeom.z > 0.98) {
                            detailColor = texture(u_DetailMap, fs_in.texCoord1);
                            detailNormalColor.rgb = texture(u_DetailNormalMap, fs_in.texCoord1).rgb * 2.0 - 1.0;
                        } else {
                            // Uniform triplanar coordinates (tile every 16 meters)
                            vec3 coord = vec3(worldPos, fs_in.height) / 16.0;

                            // Calculate continuous derivatives from top-down UVs to prevent seams at projection boundaries
                            vec2 ddx = dFdx(fs_in.texCoord1);
                            vec2 ddy = dFdy(fs_in.texCoord1);

                            detailColor = textureGrad(u_DetailMap, fs_in.texCoord1, ddx, ddy) * blendWeights.z +
                                          textureGrad(u_DetailMap, coord.yz, ddx, ddy) * blendWeights.x +
                                          textureGrad(u_DetailMap, coord.xz, ddx, ddy) * blendWeights.y;

                            vec3 nXY = textureGrad(u_DetailNormalMap, fs_in.texCoord1, ddx, ddy).rgb * 2.0 - 1.0;
                            vec3 nYZ = textureGrad(u_DetailNormalMap, coord.yz, ddx, ddy).rgb * 2.0 - 1.0;
                            vec3 nXZ = textureGrad(u_DetailNormalMap, coord.xz, ddx, ddy).rgb * 2.0 - 1.0;

                            // Rotate side normals to world space based on projection plane and face direction
                            detailNormalColor.rgb = normalize(nXY * blendWeights.z +
                                                              vec3(nYZ.z * sign(worldNormalGeom.x), nYZ.x, nYZ.y) * blendWeights.x +
                                                              vec3(nXZ.x, nXZ.z * sign(worldNormalGeom.y), nXZ.y) * blendWeights.y);
                        }

                        // Subtly modulate diffuse color with detail noise
                        float slope = 1.0 - worldNormalGeom.z;
                        vec3 srgbDiffuse = pow(diffuseColor.rgb, vec3(1.0 / 2.2));
                        float detailFade = clamp(detailColor.a / 0.15, 0.0, 1.0);
                        float detailStrength = mix(0.15, 0.35, slope) * normalMapStrength * detailFade;
                        float detailOffset = 1.0 - detailStrength * 0.5;
                        srgbDiffuse *= (detailColor.rgb * detailStrength + detailOffset);
                        diffuseColor.rgb = pow(srgbDiffuse, vec3(2.2));

                        // Apply dynamic wetness darkening (wet surfaces scatter less light)
                        diffuseColor.rgb = mix(diffuseColor.rgb, diffuseColor.rgb * 0.55, wetness);

                        vec3 viewNormalGeom = normalize((u_viewMatrix * vec4(worldNormalGeom, 0.0)).xyz);
                        vec3 viewNormal = normalize((u_viewMatrix * vec4(worldNormal, 0.0)).xyz);

                        // Reduce the normal map perturbation strength under water to smooth the underwater terrain
                        baseNormal = normalize(mix(viewNormalGeom, baseNormal, normalMapStrength));

                        // Blend baseNormal back to the flat viewNormal near the boundary to eliminate TBN/derivative mismatches
                        baseNormal = normalize(mix(viewNormal, baseNormal, edgeBlend));

                        // Micro-detail normal perturbation from detail map color (adds tactile depth up close)
                        // Extract deviation from "flat" geometry normal and transform to view space
                        vec3 detailDeviation = detailNormalColor.rgb - worldNormalGeom;
                        vec3 vDetailNormal = (u_viewMatrix * vec4(detailDeviation, 0.0)).xyz;
                        float detailNormalStrength = mix(0.20, 0.02, wetness) * normalMapStrength * detailFade;
                        vec3 normal = normalize(baseNormal + vDetailNormal * detailNormalStrength);

                        // Dynamic specular (Blinn-Phong) & rim lighting
                        vec3 viewDir = normalize(-fs_in.viewPosition);
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

                        float fogFactor = calculateFogFactor(fs_in.fogDist, gl_FragCoord.xy);
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
