package com.oddlabs.tt.scenery;

import com.oddlabs.tt.engine.render.shader.FogShader;
import com.oddlabs.tt.engine.render.shader.LitShader;
import com.oddlabs.tt.engine.render.shader.ShaderProgram;

/**
 * Renders the dynamic 3D landscape with terrain texturing, normal mapping, and lighting.
 */
final class LandscapeShader extends ShaderProgram implements FogShader, LitShader {

    private interface Uniforms {
        String HEIGHT_MAP = "u_HeightMap";
        String DIFFUSE_MAP = "u_DiffuseMap";
        String NORMAL_MAP = "u_NormalMap";
        String DETAIL_MAP = "u_DetailMap";
        String DETAIL_NORMAL_MAP = "u_DetailNormalMap";
        String WORLD_SIZE = "u_WorldSize";
        String DETAIL_SCALE = "u_DetailScale";
        String SEA_BOTTOM_COLOR = "u_SeaBottomColor";
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
                        vec3 viewDir = normalize(-fs_in.viewPosition);
                        vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection.xyz, 0.0)).xyz);

                        vec4 diffuseColor = texture(u_DiffuseMap, fs_in.texCoordColormap);
                        vec4 detailColor;

                        // Surface roughness metadata baked into diffuse alpha (1.0 = rough, 0.0 = smooth)
                        float roughness = diffuseColor.a;

                        // Reconstruct world position and calculate dynamic wetness factor
                        vec2 worldPos = fs_in.texCoordColormap * u_WorldSize;
                        float waveHeight = getWaveHeight(worldPos) * fs_in.waveScale;
                        float u_seaLevel = u_fogParams.w;

                        // Add a slow tide oscillation to the water height for the ocean wash effect
                        float tide = sin(u_waveTime * 0.25) * 0.15 * fs_in.waveScale;
                        float waterHeight = u_seaLevel + waveHeight + tide;
                        float depth = waterHeight - fs_in.height;
                        float wetness = clamp((depth + 0.10) / 0.30, 0.0, 1.0);

                        // Seamless transition to SeaBottom at the world perimeter
                        float distToEdgeX = min(fs_in.texCoordColormap.x, 1.0 - fs_in.texCoordColormap.x);
                        float distToEdgeY = min(fs_in.texCoordColormap.y, 1.0 - fs_in.texCoordColormap.y);
                        float distToEdge = min(distToEdgeX, distToEdgeY);
                        float edgeBlend = smoothstep(0.0, 0.04, distToEdge);

                        // Blend colormap diffuse to linear sea bottom color towards the world border
                        diffuseColor.rgb = mix(u_SeaBottomColor, diffuseColor.rgb, edgeBlend);

                        // Compute view-space normal from heightmap slope, flattening towards the boundary
                        float h_plus_x = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(1, 0)).r;
                        float h_minus_x = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(-1, 0)).r;
                        float h_plus_y = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(0, 1)).r;
                        float h_minus_y = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(0, -1)).r;

                        // Calculate normal for specular highlights
                        vec3 worldNormal = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 64.0));
                        worldNormal = normalize(mix(vec3(0.0, 0.0, 1.0), worldNormal, edgeBlend));

                        // Sample detail map using planar coordinates (matching legacy)
                        detailColor = texture(u_DetailMap, fs_in.texCoord1);

                        // Decal blending in display/sRGB space matching legacy GL_DECAL contract.
                        // Blending in sRGB prevents linear saturation wash-out, and using raw detailColor.a
                        // preserves the natural trilinear mipmap distance fadeout without crumpled paper creases.
                        vec3 srgbDiffuse = pow(diffuseColor.rgb, vec3(1.0 / 2.2));
                        vec3 srgbMixed = mix(srgbDiffuse, detailColor.rgb, detailColor.a);
                        diffuseColor.rgb = pow(srgbMixed, vec3(2.2));

                        vec3 viewNormal = normalize((u_viewMatrix * vec4(worldNormal, 0.0)).xyz);
                        vec3 normal = viewNormal;

                        // Dynamic specular (Blinn-Phong) & rim lighting
                        vec3 halfDir = normalize(lightDir + viewDir);

                        // Dry terrain has zero specular; only wet surfaces gain water-film specular highlights
                        float drySpecIntensity = 0.0;
                        float wetSpecIntensity = 0.05;

                        // Specular exponent sharpened by surface smoothness (low roughness)
                        float specExponent = mix(128.0, 32.0, roughness);
                        specExponent = mix(specExponent, 80.0, wetness);

                        float specIntensity = mix(drySpecIntensity, wetSpecIntensity, wetness);

                        // Fresnel reflection (Schlick approximation)
                        float fresnelBase = mix(0.04, 0.20, wetness);
                        float fresnel = fresnelBase + (1.0 - fresnelBase) * pow(clamp(1.0 - dot(normal, viewDir), 0.0, 1.0), 5.0);
                        specIntensity = mix(specIntensity, specIntensity * 2.0, fresnel * (1.0 - roughness));

                        float spec = pow(max(dot(normal, halfDir), 0.0), specExponent);
                        vec3 specular = specIntensity * spec * vec3(1.0) * edgeBlend;

                        // Terrain lighting is fully baked into the colormap texture (BlendLighting sun highlights
                        // and shadowcasting). Avoiding redundant runtime Half-Lambert/ambient modulation preserves
                        // the vibrant legacy color aesthetic and prevents faceted heightmap creases.
                        // Submerged wetness darkening transitions off at the world edge to match SeaBottom luminance.
                        diffuseColor.rgb = mix(diffuseColor.rgb, diffuseColor.rgb * 0.55, wetness * edgeBlend);

                        // --- Dynamic Shoreline "Wet Line" (Wash/Foam) ---
                        // Brighten the leading edge of the water to simulate foam and bubbles
                        float wash = smoothstep(0.0, 0.08, depth) * (1.0 - smoothstep(0.12, 0.25, depth));
                        diffuseColor.rgb = mix(diffuseColor.rgb, diffuseColor.rgb + vec3(0.15, 0.2, 0.25), wash * 0.6 * fs_in.waveScale);

                        vec3 litColor = diffuseColor.rgb + specular * 1.1;

                        // --- Underwater Caustics ---
                        float depthStatic = u_seaLevel - fs_in.height;
                        if (depth > 0.0 && depthStatic > 0.0 && fs_in.waveScale > 0.01) {
                            float causticsTime = u_waveTime * 0.05;
                            vec2 uv1 = worldPos * 0.15 + vec2(causticsTime * 0.08, causticsTime * 0.05);
                            vec2 uv2 = worldPos * 0.12 - vec2(causticsTime * 0.06, causticsTime * 0.10);

                            float h1 = getWaveHeight(uv1);
                            float h2 = getWaveHeight(uv2);

                            float c = 1.0 - abs(h1 - h2);
                            float caustic = pow(max(0.0, c), 16.0);

                            float depthFade = smoothstep(0.0, 0.15, depth) * clamp(1.0 - depthStatic / 3.5, 0.0, 1.0);
                            float causticFactor = caustic * depthFade * (1.0 - roughness * 0.4) * fs_in.waveScale;
                            litColor *= (1.0 + causticFactor * 0.35);
                        }

                        vec3 finalColor = applyFog(litColor, fs_in.fogDist, gl_FragCoord.xy);
                        out_FragColor = vec4(finalColor, 1.0);
                    }
                    """;

    final int locHeightMap;
    final int locDiffuseMap;
    final int locNormalMap;
    final int locDetailMap;
    final int locDetailNormalMap;
    final int locWorldSize;
    final int locDetailScale;
    final int locSeaBottomColor;

    LandscapeShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
        locHeightMap = getUniformLocation(Uniforms.HEIGHT_MAP);
        locDiffuseMap = getUniformLocation(Uniforms.DIFFUSE_MAP);
        locNormalMap = getUniformLocation(Uniforms.NORMAL_MAP);
        locDetailMap = getUniformLocation(Uniforms.DETAIL_MAP);
        locDetailNormalMap = getUniformLocation(Uniforms.DETAIL_NORMAL_MAP);
        locWorldSize = getUniformLocation(Uniforms.WORLD_SIZE);
        locDetailScale = getUniformLocation(Uniforms.DETAIL_SCALE);
        locSeaBottomColor = getUniformLocation(Uniforms.SEA_BOTTOM_COLOR);
    }
}
