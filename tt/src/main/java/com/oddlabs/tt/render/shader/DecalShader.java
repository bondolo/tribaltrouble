package com.oddlabs.tt.render.shader;

/**
 * Shader program for rendering instanced decals with optional fog and radial distortion.
 */
public final class DecalShader extends ShaderProgram implements FogShader {

    public static final class Uniforms {
        public static final String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        public static final String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        public static final String TEXTURES = "u_textures";
        public static final String HEIGHT_MAP = "u_HeightMap";
        public static final String WORLD_SIZE = "u_WorldSize";
        public static final String DEPTH_BIAS = "u_DepthBias";

        private Uniforms() {
        }
    }

    public static final class Attributes {
        public static final String POSITION = Shader.POSITION;
        public static final String INSTANCE_POS = "in_InstancePos";
        public static final String INSTANCE_SIZE = "in_InstanceSize";
        public static final String INSTANCE_COLOR = "in_InstanceColor";
        public static final String INSTANCE_PATTERN = "in_InstancePattern";
        public static final String INSTANCE_OFFSET_SCALE = "in_InstanceOffsetScale";
        public static final String INSTANCE_TEX_SLOT = "in_InstanceTextureSlot";
        public static final String INSTANCE_FLAGS = "in_InstanceFlags";
        public static final String INSTANCE_SHADOW_OPACITY = "in_InstanceShadowOpacity";

        private Attributes() {
        }
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec2 in_Position;      // Grid vertex (-0.5 to 0.5)
                    layout(location = 4) in vec2 in_InstancePos;   // World X, Y
                    layout(location = 5) in float in_InstanceSize; // Size in meters
                    layout(location = 3) in vec4 in_InstanceColor; // RGBA
                    layout(location = 6) in float in_InstancePattern; // Pattern ID
                    layout(location = 7) in float in_InstanceOffsetScale; // Offset scale
                    layout(location = 8) in float in_InstanceTextureSlot; // Texture unit (0..15)
                    layout(location = 9) in float in_InstanceFlags; // bit 0: radial
                    layout(location = 10) in float in_InstanceShadowOpacity;

                    uniform mat4 u_modelViewMatrix;
                    uniform float u_WorldSize;
                    uniform float u_DepthBias;
                    uniform sampler2D u_HeightMap;

                    out vec2 v_TexCoord;
                    flat out vec2 v_ShadowOffset;
                    out vec4 v_Color;
                    out float v_Pattern;
                    flat out int v_TexSlot;
                    flat out int v_Flags;
                    out float v_ShadowOpacity;
                    out float v_fogDist;

                    void main() {
                        vec2 localPos = in_Position * in_InstanceSize;
                        vec2 worldPos = in_InstancePos + localPos;

                        // Map world position to heightmap UV
                        // Add half-texel offset to align vertex-centered heightmap (1 grid unit = 2 meters)
                        vec2 mapUV = (worldPos + 1.0) / u_WorldSize;
                        float h = texture(u_HeightMap, mapUV).r;

                        vec4 viewPosition = u_modelViewMatrix * vec4(worldPos, h, 1.0);
                        viewPosition.z += u_DepthBias;

                        gl_Position = u_projectionMatrix * viewPosition;
                        v_fogDist = length(viewPosition.xyz);

                        // Pass local grid position (-0.5..0.5) to fragment shader
                        v_TexCoord = in_Position;
                        v_Color = in_InstanceColor;
                        v_Pattern = in_InstancePattern;
                        v_TexSlot = int(in_InstanceTextureSlot + 0.5);
                        v_Flags = int(in_InstanceFlags + 0.5);
                        v_ShadowOpacity = in_InstanceShadowOpacity;

                        // Shadow offset in local decal space (±0.5 = quad edge = one radius).
                        // The shadow sample is shifted by this amount; the ring sample is not.
                        if ((v_Flags & 1) != 0) { // Radial flag
                            const vec3 lightDir = vec3(-0.70710678, 0.0, 0.70710678);
                            v_ShadowOffset = lightDir.xy * 0.225 * in_InstanceOffsetScale;
                        } else {
                            v_ShadowOffset = vec2(0.0);
                        }
                    }
                    """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ +
            GLOBAL_STATE_BLOCK +
            FOG_FUNCTION +
            """

                    uniform sampler2D u_textures[14];

                    in vec2 v_TexCoord;
                    flat in vec2 v_ShadowOffset;
                    in vec4 v_Color;
                    in float v_Pattern;
                    flat in int v_TexSlot;
                    flat in int v_Flags;
                    in float v_ShadowOpacity;
                    out float v_fogDist;

                    layout(location = 0) out vec4 out_FragColor;
                    layout(location = 1) out vec4 out_MaskColor;

                    vec4 sampleDecal(vec2 uv) {
                        return texture(u_textures[v_TexSlot], uv);
                    }

                    void main() {
                        vec4 baseColor;
                        if ((v_Flags & 1) != 0) { // Radial flag
                            // Ring: sample at unshifted position so it stays centred on the unit.
                            float dist = length(v_TexCoord) * 2.5;
                            float time = u_globalTime;
                            float angle = atan(v_TexCoord.y, v_TexCoord.x);

                            // LUT specialized channel mapping:
                            // Red   = Ring Alpha
                            // Green = Shadow Alpha

                            // Shadow: sampled at a position offset opposite the light direction
                            // so the blob falls behind the unit while the ring stays centred.
                            float shadowDist = length(v_TexCoord - v_ShadowOffset) * 2.5;
                            float shadowAlpha = sampleDecal(vec2(shadowDist * 2.0, 0.5)).g;

                            vec4 baseSample = sampleDecal(vec2(dist * 2.0, 0.5));

                            float ringAlpha = 0.0;

                            if (v_Pattern > 0.5) { // Any active pattern (Selection/Target)
                                if (v_Pattern < 1.5) { // Pattern 1: Friendly (Throb)
                                    float offset = 0.03 * sin(time * 4.0);
                                    ringAlpha = sampleDecal(vec2((dist - offset) * 2.0, 0.5)).r;
                                }
                                else if (v_Pattern < 2.5) { // Pattern 2: Neutral/Ally (Marching Ants)
                                    float ants = step(0.5, fract(angle * 10.0 / 6.28318 + time * 2.0));
                                    ringAlpha = baseSample.r * (0.4 + 0.6 * ants);
                                }
                                else if (v_Pattern < 3.5) { // Pattern 3: Enemy (Aggressive Double Ring)
                                    float o1 = 0.03 * sin(time * 12.0);
                                    float r1 = sampleDecal(vec2((dist - o1) * 2.0, 0.5)).r;
                                    float r2 = sampleDecal(vec2((dist + 0.12) * 2.0, 0.5)).r;
                                    ringAlpha = max(r1, r2);
                                }
                                else if (v_Pattern < 4.5) { // Pattern 4: Friendly Building (Minimal Throb)
                                    float offset = 0.01 * sin(time * 2.0);
                                    ringAlpha = sampleDecal(vec2((dist - offset) * 2.0, 0.5)).r;
                                }
                                else if (v_Pattern < 5.5) { // Pattern 5: Neutral Building (Static Ring + Marching Ants)
                                    float ants = step(0.5, fract(angle * 15.0 / 6.28318 + time * 1.0));
                                    ringAlpha = baseSample.r * (0.4 + 0.6 * ants);
                                }
                                else { // Pattern 6: Enemy Building (Double Ring + Minimal Throb)
                                    float o1 = 0.01 * sin(time * 2.0);
                                    float r1 = sampleDecal(vec2((dist - o1) * 2.0, 0.5)).r;
                                    float r2 = sampleDecal(vec2((dist + 0.12) * 2.0, 0.5)).r;
                                    ringAlpha = max(r1, r2);
                                }
                            } else {
                                ringAlpha = baseSample.r;
                            }

                            // Composition: Apply Ring OVER Shadow
                            float a_r = ringAlpha * 0.8 * v_Color.a; // 80% opacity for selection rings
                            float a_s = shadowAlpha * v_ShadowOpacity;     // opacity scaled by shadow opacity

                            float finalAlpha = a_r + a_s * (1.0 - a_r);

                            // Final output is premultiplied:
                            // RGB comes only from the ring (tinted). Shadow is black (adds 0 to RGB).
                            baseColor = vec4(v_Color.rgb * a_r, finalAlpha);
                        } else {
                            // Standard 2D sampling (Square Building Sites)
                            baseColor = sampleDecal(v_TexCoord + 0.5) * v_Color;
                            if (v_Pattern > 9.5) {
                                float dist = length(v_TexCoord);
                                float maskRadius = v_Pattern - 10.0;
                                float edgeWidth = 0.05;
                                float alphaScale = 1.0 - smoothstep(maskRadius - edgeWidth, maskRadius, dist);
                                baseColor.a *= alphaScale;
                                if (maskRadius > 0.0) {
                                    float t = clamp(dist / maskRadius, 0.0, 1.0);
                                    vec3 glowColor = vec3(2.0, 0.4, 0.0) * (1.0 - t) * (1.0 - t);
                                    baseColor.rgb = glowColor * v_Color.rgb * baseColor.a;
                                } else {
                                    baseColor.rgb = vec3(0.0);
                                }
                            } else {
                                baseColor.rgb *= baseColor.a; // Premultiply for consistency
                            }
                        }

                        float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                        // For premultiplied alpha, the fog color must also be weighted by alpha
                        // to correctly blend with the background.
                        vec3 litColor = mix(u_fogColor.rgb * baseColor.a, baseColor.rgb, fogFactor);
                        out_FragColor = vec4(litColor, baseColor.a);

                        // Shadows/Decals NEVER mark the mask buffer.
                        out_MaskColor = vec4(0.0);
                    }
                    """;

    public DecalShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}
