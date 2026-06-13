package com.oddlabs.tt.render.shader;

/**
 * Shader for full-screen post-processing effects.
 * Handles Color Vision Deficiency (CVD) correction and High Contrast Mode.
 */
public final class PostProcessShader extends ShaderProgram {

    public interface Uniforms {
        String SCENE_TEXTURE = "u_sceneTexture";
        String MASK_TEXTURE = "u_maskTexture";
        String CVD_INTENSITY = "u_cvdIntensity";
        String CONTRAST_INTENSITY = "u_contrastIntensity";
        String INVERT_COLORS = "u_invertColors";
        String CONTRAST_BRIGHTNESS = "u_contrastBrightness";
        String CONTRAST_CLARITY = "u_contrastClarity";
        String TEAM_STENCIL = "u_teamStencil";
    }

    public interface Attributes {
        String POSITION = "in_Position";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            """
                    layout(location = 0) in vec2 in_Position;

                    out vec2 v_texCoord;

                    void main() {
                        // Full screen quad coordinates: -1 to 1
                        gl_Position = vec4(in_Position, 0.0, 1.0);
                        v_texCoord = (in_Position + 1.0) * 0.5;
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            COLOR_SPACE_FUNCTIONS +
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

                    // --- Subroutine Definitions ---
                    subroutine vec3 CvdFilter(vec3 color);
                    subroutine uniform CvdFilter u_cvdFilter;

                    subroutine vec3 ContrastFilter(vec3 color, float maskAlpha);
                    subroutine uniform ContrastFilter u_contrastFilter;

                    // --- CVD Logic ---
                    // LMS Colour Space Matrices (Transposed for GLSL Column-Major)
                    const mat3 RGB_to_LMS = mat3(
                        17.8824, 3.45565, 0.0299566,
                        43.5161, 27.1554, 0.184309,
                        4.11935, 3.86714, 1.46709
                    );

                    const mat3 LMS_to_RGB = mat3(
                        0.0809, -0.0102, -0.0003,
                        -0.1305, 0.0540, -0.0041,
                        0.1167, -0.1136, 0.6935
                    );

                    // Simulation Matrices (Transposed for GLSL Column-Major)
                    const mat3 Protanopia_Sim = mat3(
                        0.0, 0.0, 0.0,
                        2.02344, 1.0, 0.0,
                        -2.52581, 0.0, 1.0
                    );

                    const mat3 Deuteranopia_Sim = mat3(
                        1.0, 0.494207, 0.0,
                        0.0, 0.0, 0.0,
                        0.0, 1.24827, 1.0
                    );

                    const mat3 Tritanopia_Sim = mat3(
                        1.0, 0.0, -0.395913,
                        0.0, 1.0, 0.801109,
                        0.0, 0.0, 0.0
                    );

                    subroutine(CvdFilter) vec3 cvdNone(vec3 color) {
                        return color;
                    }

                    subroutine(CvdFilter) vec3 cvdProtanopia(vec3 color) {
                        vec3 lms = RGB_to_LMS * color;
                        vec3 simulatedLMS = Protanopia_Sim * lms;
                        vec3 simulatedRGB = LMS_to_RGB * simulatedLMS;
                        vec3 error = color - simulatedRGB;
                        // Protanopia: Shift R/G error to Blue channel
                        vec3 correction = vec3(0.0, 0.0, (error.r * 0.7) + (error.g * 0.7));
                        return color + correction * u_cvdIntensity;
                    }

                    subroutine(CvdFilter) vec3 cvdDeuteranopia(vec3 color) {
                        vec3 lms = RGB_to_LMS * color;
                        vec3 simulatedLMS = Deuteranopia_Sim * lms;
                        vec3 simulatedRGB = LMS_to_RGB * simulatedLMS;
                        vec3 error = color - simulatedRGB;
                        // Deuteranopia: Shift R/G error to Blue channel
                        vec3 correction = vec3(0.0, 0.0, (error.r * 0.7) + (error.g * 0.7));
                        return color + correction * u_cvdIntensity;
                    }

                    subroutine(CvdFilter) vec3 cvdTritanopia(vec3 color) {
                        vec3 lms = RGB_to_LMS * color;
                        vec3 simulatedLMS = Tritanopia_Sim * lms;
                        vec3 simulatedRGB = LMS_to_RGB * simulatedLMS;
                        vec3 error = color - simulatedRGB;
                        // Tritanopia: Shift B error to Red/Green channels
                        vec3 correction = vec3(error.b * 0.7, error.b * 0.7, 0.0);
                        return color + correction * u_cvdIntensity;
                    }

                    // --- High Contrast & Accessibility Logic ---
                    subroutine(ContrastFilter) vec3 contrastNone(vec3 color, float maskAlpha) {
                        return color;
                    }

                    subroutine(ContrastFilter) vec3 contrastApply(vec3 color, float maskAlpha) {
                        vec3 result = color;

                        // 1. Edge Clarity (Unsharp Mask)
                        if (u_contrastClarity > 0.01) {
                            vec2 texelSize = 1.0 / textureSize(u_sceneTexture, 0);
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

                        // Rational Sigmoid: f(x) = x / (1 + |x|)
                        vec3 centered = result - pivot;
                        vec3 sigmoid = (centered * k) / (1.0 + abs(centered * k)) + pivot;

                        // Mix with original to ensure identity at u_contrastIntensity == 0
                        // and to prevent the "white tinge" at low intensities.
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

                        // Apply Accessibility Filters
                        float maskAlpha = u_teamStencil ? mask.a : 0.0;
                        finalColor = u_contrastFilter(finalColor, maskAlpha);

                        // Team Stencil Overlay (Linear Space)
                        if (u_teamStencil) {
                            // GUI pixels use alpha=0.5 in the mask buffer.
                            bool isGui = abs(mask.a - 0.5) < 0.1;

                            if (!isGui) {
                                // Team objects write alpha=1.0. Clear colour is alpha=0.0.
                                if (mask.a > 0.9 && dot(mask.rgb, vec3(1.0)) > 0.01) {
                                    finalColor = mix(finalColor, mask.rgb, 0.2);
                                } else {
                                    vec2 texelSize = 1.0 / vec2(textureSize(u_maskTexture, 0));
                                    int maskCount = 0;
                                    vec3 accumulatedColor = vec3(0.0);

                                    for (int y = -4; y <= 4; y++) {
                                        for (int x = -4; x <= 4; x++) {
                                            if (x == 0 && y == 0) continue;

                                            vec4 neighbor = texture(u_maskTexture, v_texCoord + vec2(float(x), float(y)) * texelSize);
                                            if (dot(neighbor.rgb, vec3(1.0)) > 0.01) {
                                                maskCount++;
                                                accumulatedColor += neighbor.rgb;
                                            }
                                        }
                                    }

                                    if (maskCount > 0) {
                                        finalColor = accumulatedColor / float(maskCount);
                                    }
                                }
                            }
                        }

                        // 4. Apply CVD correction
                        finalColor = u_cvdFilter(finalColor);

                        // 5. Final Output (Opaque)
                        // GL_FRAMEBUFFER_SRGB handles the conversion to sRGB for the backbuffer.
                        out_FragColor = vec4(finalColor, 1.0);
                    }
                    """;

    public PostProcessShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }

    public void setSubroutines(int cvdMode, boolean highContrast) {
        String cvdName = switch (cvdMode) {
            case 1 -> "cvdProtanopia";
            case 2 -> "cvdDeuteranopia";
            case 3 -> "cvdTritanopia";
            default -> "cvdNone";
        };

        String contrastName = highContrast ? "contrastApply" : "contrastNone";

        setFragmentSubroutines(java.util.Map.of(
                "u_cvdFilter", cvdName,
                "u_contrastFilter", contrastName
        ));
    }
}
