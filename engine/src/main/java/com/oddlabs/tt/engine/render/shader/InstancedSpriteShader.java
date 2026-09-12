package com.oddlabs.tt.engine.render.shader;

/**
 * Renders 3D sprites using hardware instancing and skeletal skinning.
 * Supports GPU 4-bone linear blend skinning and a direct static path for rigid geometry.
 */
public final class InstancedSpriteShader extends ShaderProgram implements FogShader, LitShader {

    private interface Uniforms {
        String TEXTURE_0 = "u_texture0";
        String TEXTURE_1 = "u_texture1";
        String NORMAL_MAP = "u_normalMap";
        String BONE_MATRIX_BUFFER = "u_BoneMatrixBuffer";
        String ENABLE_LIGHTING = "u_enableLighting";
        String ENABLE_TEAM_COLOR = "u_enableTeamColor";
        String ENABLE_NORMAL_MAP = "u_enableNormalMap";
        String MODULATE_COLOR = "u_modulateColor";
        String REPLACE_MODE = "u_replaceMode";
        String DESATURATE = "u_desaturate";
        String ALPHA_TEST_VALUE = "u_alphaTestValue";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                        layout(location = 0) in vec3 in_Position;
                        layout(location = 1) in vec3 in_Normal;
                        layout(location = 2) in vec2 in_TexCoord;
                        layout(location = 3) in uvec4 in_BoneIndices;

                        // Per-instance
                        layout(location = 4) in mat4 in_InstanceModelMatrix;
                        layout(location = 8) in vec4 in_InstanceColor;
                        layout(location = 9) in vec4 in_InstanceDecalColor;
                        layout(location = 10) in float in_BoneBaseOffset;

                        layout(location = 11) in vec4 in_BoneWeights;

                        uniform samplerBuffer u_BoneMatrixBuffer;

                        out VS_OUT {
                            vec2 texCoord0;
                            vec4 color;
                            vec4 decalColor;
                            float fogDist;
                            vec3 viewPosition;
                            vec3 viewNormal;
                            vec3 worldNormal;
                        } vs_out;

                        mat4 fetchBoneMatrix(uint boneIndex) {
                            int base = int(round(in_BoneBaseOffset)) + int(boneIndex) * 4;
                            return mat4(
                                texelFetch(u_BoneMatrixBuffer, base + 0),
                                texelFetch(u_BoneMatrixBuffer, base + 1),
                                texelFetch(u_BoneMatrixBuffer, base + 2),
                                texelFetch(u_BoneMatrixBuffer, base + 3)
                            );
                        }

                        void main() {
                            vec4 worldPosition;
                            vec3 normal;

                            if (in_BoneBaseOffset < 0.0) {
                                worldPosition = in_InstanceModelMatrix * vec4(in_Position, 1.0);
                                normal = in_Normal;
                            } else {
                                mat4 skinMatrix = in_BoneWeights.x * fetchBoneMatrix(in_BoneIndices.x) +
                                                  in_BoneWeights.y * fetchBoneMatrix(in_BoneIndices.y) +
                                                  in_BoneWeights.z * fetchBoneMatrix(in_BoneIndices.z) +
                                                  in_BoneWeights.w * fetchBoneMatrix(in_BoneIndices.w);

                                vec4 skinnedPos = skinMatrix * vec4(in_Position, 1.0);
                                normal = normalize(mat3(skinMatrix) * in_Normal);
                                worldPosition = in_InstanceModelMatrix * skinnedPos;
                            }

                            vec4 viewPosition = u_viewMatrix * worldPosition;
                            gl_Position = u_projectionMatrix * viewPosition;

                            vs_out.texCoord0 = in_TexCoord;
                            vs_out.color = in_InstanceColor;
                            vs_out.decalColor = in_InstanceDecalColor;
                            vs_out.fogDist = length(viewPosition.xyz);

                            vs_out.viewPosition = viewPosition.xyz;
                            vs_out.viewNormal = normalize((u_viewMatrix * in_InstanceModelMatrix * vec4(normal, 0.0)).xyz);
                            vs_out.worldNormal = normalize((in_InstanceModelMatrix * vec4(normal, 0.0)).xyz);
                        }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            FOG_FUNCTION +
            PERTURB_NORMAL_FUNC +
            FRAGMENT_LIGHTING_FUNCTION +
            """
                    uniform sampler2D u_texture0;
                    uniform sampler2D u_texture1;
                    uniform sampler2D u_normalMap;
                    uniform bool u_enableTeamColor;
                    uniform bool u_enableNormalMap;
                    uniform bool u_enableLighting;
                    uniform bool u_modulateColor;
                    uniform bool u_replaceMode;
                    // u_decalColor is now v_decalColor
                    uniform float u_desaturate;
                    uniform float u_alphaTestValue;

                    in VS_OUT {
                        vec2 texCoord0;
                        vec4 color;
                        vec4 decalColor;
                        float fogDist;
                        vec3 viewPosition;
                        vec3 viewNormal;
                        vec3 worldNormal;
                    } fs_in;

                    layout(location = 0) out vec4 out_FragColor;
                    layout(location = 1) out vec4 out_MaskColor;

                    void main() {
                        vec4 base = texture(u_texture0, fs_in.texCoord0);
                        out_MaskColor = vec4(0.0);

                        if (u_desaturate > 0.0) {
                            float gray = dot(base.rgb, vec3(0.2126, 0.7152, 0.0722));
                            vec3 ghostTarget = mix(vec3(gray), vec3(1.0), 0.9);
                            base.rgb = mix(base.rgb, ghostTarget, u_desaturate);
                        }

                        vec4 finalColor;
                        if (u_replaceMode) {
                            finalColor = base;
                        } else if (u_modulateColor) {
                            finalColor = fs_in.color * base;
                        } else {
                            // Apply lighting
                            vec3 normal = normalize(fs_in.viewNormal);
                            float specularStrength = 0.0;

                            if (u_enableNormalMap) {
                                vec4 normalMapVal = texture(u_normalMap, fs_in.texCoord0);
                                normal = perturbNormal(normal, normalize(fs_in.viewPosition), fs_in.texCoord0, normalMapVal.rgb);
                                specularStrength = normalMapVal.a;
                            }

                            vec3 lightIntensity = vec3(1.0);
                            if (u_enableLighting) {
                                lightIntensity = calculateLighting(normal, fs_in.worldNormal, fs_in.viewPosition, specularStrength);
                            }

                            // fs_in.color is the instance color (e.g. material color)
                            finalColor = vec4(fs_in.color.rgb * base.rgb * lightIntensity, fs_in.color.a * base.a);

                            if (u_enableTeamColor) {
                                vec4 tex1 = texture(u_texture1, fs_in.texCoord0);
                                // Mix decal color
                                vec3 mixedColor = mix(finalColor.rgb, fs_in.decalColor.rgb * lightIntensity, tex1.rgb);
                                finalColor.rgb = mixedColor;

                                // Write to Mask Buffer (Team Color)
                                if (base.a > 0.1) {
                                    out_MaskColor = fs_in.decalColor;
                                }
                            }
                        }

                        if (finalColor.a <= u_alphaTestValue) discard;

                        float fogFactor = calculateFogFactor(fs_in.fogDist, gl_FragCoord.xy);
                        vec3 litColor = mix(u_fogColor.rgb, finalColor.rgb, fogFactor);
                        out_FragColor = vec4(litColor, finalColor.a);
                    }
                    """;

    public final int locTexture0;
    public final int locTexture1;
    public final int locNormalMap;
    public final int locBoneMatrixBuffer;
    public final int locEnableLighting;
    public final int locEnableTeamColor;
    public final int locEnableNormalMap;
    public final int locModulateColor;
    public final int locReplaceMode;
    public final int locDesaturate;
    public final int locAlphaTestValue;

    public InstancedSpriteShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
        locTexture0 = getUniformLocation(Uniforms.TEXTURE_0);
        locTexture1 = getUniformLocation(Uniforms.TEXTURE_1);
        locNormalMap = getUniformLocation(Uniforms.NORMAL_MAP);
        locBoneMatrixBuffer = getUniformLocation(Uniforms.BONE_MATRIX_BUFFER);
        locEnableLighting = getUniformLocation(Uniforms.ENABLE_LIGHTING);
        locEnableTeamColor = getUniformLocation(Uniforms.ENABLE_TEAM_COLOR);
        locEnableNormalMap = getUniformLocation(Uniforms.ENABLE_NORMAL_MAP);
        locModulateColor = getUniformLocation(Uniforms.MODULATE_COLOR);
        locReplaceMode = getUniformLocation(Uniforms.REPLACE_MODE);
        locDesaturate = getUniformLocation(Uniforms.DESATURATE);
        locAlphaTestValue = getUniformLocation(Uniforms.ALPHA_TEST_VALUE);
    }
}
