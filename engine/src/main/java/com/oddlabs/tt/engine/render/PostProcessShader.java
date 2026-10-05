package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.engine.render.shader.ShaderProgram;

/**
 * Renders full-screen post-processing effects for Color Vision Deficiency (CVD) correction and High Contrast Mode.
 */
final class PostProcessShader extends ShaderProgram {

    private interface Uniforms {
        String SCENE_TEXTURE = "u_sceneTexture";
        String MASK_TEXTURE = "u_maskTexture";
        String CVD_MODE = "u_cvdMode";
        String CVD_INTENSITY = "u_cvdIntensity";
        String HIGH_CONTRAST = "u_highContrast";
        String CONTRAST_INTENSITY = "u_contrastIntensity";
        String INVERT_COLORS = "u_invertColors";
        String CONTRAST_BRIGHTNESS = "u_contrastBrightness";
        String CONTRAST_CLARITY = "u_contrastClarity";
        String TEAM_STENCIL = "u_teamStencil";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            """
                    out vec2 v_texCoord;

                    void main() {
                        vec2 uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                        v_texCoord = uv;
                        gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            """
                    uniform sampler2D u_sceneTexture;
                    uniform sampler2D u_maskTexture;
                    uniform int u_cvdMode; // 0=None, 1=Protanopia, 2=Deuteranopia, 3=Tritanopia
                    uniform float u_cvdIntensity;
                    uniform bool u_highContrast;
                    uniform float u_contrastIntensity;
                    uniform bool u_invertColors;
                    uniform float u_contrastBrightness;
                    uniform float u_contrastClarity;
                    uniform bool u_teamStencil;

                    in vec2 v_texCoord;
                    layout(location = 0) out vec4 out_FragColor;

                    // --- CVD Daltonization Logic ---
                    // Linear RGB simulation matrices (T_inv * S * T) using Hunt-Pointer-Estevez LMS.
                    // Constructed column-major: mat3(col0, col1, col2)

                    // Protanopia simulation (L-cone deficiency)
                    const mat3 PROTANOPIA_SIM = mat3(
                        vec3(0.170557, 0.170557, -0.004517),
                        vec3(0.829443, 0.829443,  0.004517),
                        vec3(0.0,      0.0,       1.0)
                    );

                    // Deuteranopia simulation (M-cone deficiency)
                    const mat3 DEUTERANOPIA_SIM = mat3(
                        vec3(0.330660, 0.330660, -0.027855),
                        vec3(0.669340, 0.669340,  0.027855),
                        vec3(0.0,      0.0,       1.0)
                    );

                    // Tritanopia simulation (S-cone deficiency)
                    const mat3 TRITANOPIA_SIM = mat3(
                        vec3( 1.0,       0.0,       0.0),
                        vec3( 0.127399,  0.873909,  0.873909),
                        vec3(-0.127399,  0.126091,  0.126091)
                    );

                    vec3 applyCvdFilter(vec3 color) {
                        if (u_cvdMode == 0 || u_cvdIntensity <= 0.001) {
                            return color;
                        }

                        // Anomalous Trichromacy Simulation:
                        // Models partial cone deficiency severity s in [0, 1] by interpolating between Identity and Dichromacy.
                        // Since c - ((1 - s)I + s M)c == s(c - Mc), we compute error directly against the constant dichromacy matrix.
                        float s = clamp(u_cvdIntensity, 0.0, 1.0);

                        vec3 correction;
                        if (u_cvdMode == 1) {
                            // Protanopia / Protanomaly (L-cone deficiency)
                            vec3 error = (color - PROTANOPIA_SIM * color) * s;

                            // Shift lost red error into green and blue channels.
                            // Add red luminance compensation (+0.3 max(error.r, 0.0)) to offset photopic L-cone luminous loss.
                            float redLumBoost = 0.3 * max(error.r, 0.0);
                            correction = vec3(0.0, 0.7 * error.r + redLumBoost, 0.7 * error.r);
                        } else if (u_cvdMode == 2) {
                            // Deuteranopia / Deuteranomaly (M-cone deficiency)
                            vec3 error = (color - DEUTERANOPIA_SIM * color) * s;

                            // Shift lost green error into red and blue channels
                            correction = vec3(0.7 * error.g, 0.0, 0.7 * error.g);
                        } else if (u_cvdMode == 3) {
                            // Tritanopia / Tritanomaly (S-cone deficiency)
                            vec3 error = (color - TRITANOPIA_SIM * color) * s;

                            // Shift lost blue error into red and green channels
                            correction = vec3(0.7 * error.b, 0.7 * error.b, 0.0);
                        } else {
                            return color;
                        }

                        return clamp(color + correction, 0.0, 1.0);
                    }

                    // --- High Contrast & Accessibility Logic ---
                    vec3 applyContrastFilter(vec3 color, float maskAlpha) {
                        if (!u_highContrast) {
                            return color;
                        }
                        vec3 result = color;

                        // 1. Edge Clarity (Unsharp Mask)
                        if (u_contrastClarity > 0.01) {
                            vec2 texelSize = 1.0 / vec2(textureSize(u_sceneTexture, 0));
                            vec3 blurred = vec3(0.0);
                            // Simple 5-tap box filter for speed
                            blurred += texture(u_sceneTexture, v_texCoord + vec2(texelSize.x, 0.0)).rgb;
                            blurred += texture(u_sceneTexture, v_texCoord - vec2(texelSize.x, 0.0)).rgb;
                            blurred += texture(u_sceneTexture, v_texCoord + vec2(0.0, texelSize.y)).rgb;
                            blurred += texture(u_sceneTexture, v_texCoord - vec2(0.0, texelSize.y)).rgb;
                            blurred *= 0.25;

                            result += (result - blurred) * u_contrastClarity * 2.0;
                        }

                        // 2. Brightness Offset (Linear)
                        result += u_contrastBrightness;
                        result = clamp(result, 0.0, 1.0);

                        // 3. Luminance-Aware S-Curve Contrast
                        // Pivot at perceptual middle gray (approx 0.18 linear)
                        const float pivot = 0.18;
                        float k = 1.0 + u_contrastIntensity * 4.0; // Boost range up to 5x

                        // Normalized Rational Sigmoid preserving [0, 1] range:
                        // Guarantees f(0.0) == 0.0 and f(1.0) == 1.0 while pivoting at middle gray.
                        float minSig = (-pivot * k) / (1.0 + pivot * k);
                        float maxSig = ((1.0 - pivot) * k) / (1.0 + (1.0 - pivot) * k);
                        vec3 centered = result - pivot;
                        vec3 rawSig = (centered * k) / (1.0 + abs(centered * k));
                        vec3 sigmoid = (rawSig - minSig) / (maxSig - minSig);

                        // Mix with original based on intensity
                        result = mix(result, sigmoid, u_contrastIntensity);
                        result = clamp(result, 0.0, 1.0);

                        // 4. Smart Inversion
                        if (u_invertColors) {
                            vec3 inverted = 1.0 - result;
                            // Protect units (maskAlpha > 0.9) from inversion to maintain team recognition,
                            // but we still want them to stand out.
                            result = mix(inverted, result, maskAlpha);
                        }

                        return result;
                    }

                    void main() {
                        vec4 sceneColor = texture(u_sceneTexture, v_texCoord);
                        vec4 mask = texture(u_maskTexture, v_texCoord);

                        vec3 finalColor = sceneColor.rgb;

                        // Apply Accessibility Filters (contrast, unsharp mask, and unit-protected color inversion)
                        float maskAlpha = mask.a;
                        finalColor = applyContrastFilter(finalColor, maskAlpha);

                        // Team Stencil Overlay (Linear Space)
                        if (u_teamStencil) {
                            // Team objects write alpha=1.0. Clear colour is alpha=0.0.
                            if (mask.a > 0.9 && dot(mask.rgb, vec3(1.0)) > 0.01) {
                                finalColor = mix(finalColor, mask.rgb, 0.2);
                            } else {
                                vec2 texelSize = 1.0 / vec2(textureSize(u_maskTexture, 0));
                                float maskCount = 0.0;
                                vec3 accumulatedColor = vec3(0.0);

                                // Advanced Sampling: use textureGather to fetch 2x2 texel blocks per instruction.
                                // Prune diagonal corners (x^2 + y^2 > 18.0) to maintain an isotropic circular footprint
                                // of radius 3.5 texels while reducing gather count from 16 to 12.
                                for (float y = -3.5; y <= 3.5; y += 2.0) {
                                    for (float x = -3.5; x <= 3.5; x += 2.0) {
                                        if (x * x + y * y > 18.0) {
                                            continue;
                                        }

                                        vec2 sampleUV = v_texCoord + vec2(x, y) * texelSize;

                                        // Gather Alpha channel (3) to quickly check for team units
                                        vec4 alphas = textureGather(u_maskTexture, sampleUV, 3);

                                        // Team pixels have alpha ~ 1.0, Background has 0.0
                                        bvec4 isTeam = greaterThan(alphas, vec4(0.9));

                                        if (any(isTeam)) {
                                            // Fetch R, G, B only if we hit a team pixel in this 2x2 block
                                            vec4 r = textureGather(u_maskTexture, sampleUV, 0);
                                            vec4 g = textureGather(u_maskTexture, sampleUV, 1);
                                            vec4 b = textureGather(u_maskTexture, sampleUV, 2);

                                            vec4 teamMask = vec4(isTeam);
                                            maskCount += dot(teamMask, vec4(1.0));

                                            accumulatedColor.r += dot(r, teamMask);
                                            accumulatedColor.g += dot(g, teamMask);
                                            accumulatedColor.b += dot(b, teamMask);
                                        }
                                    }
                                }

                                if (maskCount > 0.0) {
                                    finalColor = accumulatedColor / maskCount;
                                }
                            }
                        }

                        // Apply CVD correction
                        finalColor = applyCvdFilter(finalColor);

                        // Final Output (Opaque)
                        out_FragColor = vec4(finalColor, 1.0);
                    }
                    """;

    private final int locSceneTexture;
    private final int locMaskTexture;
    final int locCvdMode;
    final int locCvdIntensity;
    final int locHighContrast;
    final int locContrastIntensity;
    final int locInvertColors;
    final int locContrastBrightness;
    final int locContrastClarity;
    final int locTeamStencil;

    PostProcessShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
        locSceneTexture = getUniformLocation(Uniforms.SCENE_TEXTURE);
        locMaskTexture = getUniformLocation(Uniforms.MASK_TEXTURE);
        locCvdMode = getUniformLocation(Uniforms.CVD_MODE);
        locCvdIntensity = getUniformLocation(Uniforms.CVD_INTENSITY);
        locHighContrast = getUniformLocation(Uniforms.HIGH_CONTRAST);
        locContrastIntensity = getUniformLocation(Uniforms.CONTRAST_INTENSITY);
        locInvertColors = getUniformLocation(Uniforms.INVERT_COLORS);
        locContrastBrightness = getUniformLocation(Uniforms.CONTRAST_BRIGHTNESS);
        locContrastClarity = getUniformLocation(Uniforms.CONTRAST_CLARITY);
        locTeamStencil = getUniformLocation(Uniforms.TEAM_STENCIL);

        try (var _ = use()) {
            setUniform(locSceneTexture, 0);
            setUniform(locMaskTexture, 1);
        }
    }

    void setAccessibilityModes(int cvdMode, boolean highContrast) {
        setUniform(locCvdMode, cvdMode);
        setUniform(locHighContrast, highContrast);
    }
}
