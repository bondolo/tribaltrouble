package com.oddlabs.tt.scenery;

import com.oddlabs.tt.engine.render.shader.Shader;
import com.oddlabs.tt.engine.render.shader.ShaderProgram;

/**
 * Renders the sky dome with two scrolling cloud layers.
 */
final class SkyShader extends ShaderProgram {

    private interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.Uniforms.MODEL_VIEW_MATRIX;
        String TEXTURE_0 = "u_texture0"; // Outer clouds (texCoord0)
        String TEXTURE_1 = "u_texture1"; // Inner clouds (texCoord1)
        String OUTER_OFFSET = "u_outerOffset";
        String INNER_OFFSET = "u_innerOffset";
        String SKY_COLOR = "u_skyColor";
        String DOME_CENTER = "u_domeCenter";
        String CLOUD_SHADOW = "u_cloudShadow";
        String HORIZON_CLOUD_FADE = "u_horizonCloudFade";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec3 in_Position;
                    layout(location = 1) in vec2 in_TexCoord0;
                    layout(location = 2) in vec2 in_TexCoord1;
                    layout(location = 3) in vec4 in_Color;

                    uniform mat4 u_modelViewMatrix;
                    uniform vec2 u_outerOffset;
                    uniform vec2 u_innerOffset;
                    uniform vec3 u_domeCenter;

                    out VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoord1;
                        vec3 color;
                        float elevation;
                        float sunFactor;
                    } vs_out;

                    void main() {
                        gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(in_Position, 1.0);

                        vs_out.texCoord0 = in_TexCoord0 + u_outerOffset;
                        vs_out.texCoord1 = in_TexCoord1 + u_innerOffset;
                        vs_out.color = in_Color.rgb;
                        vs_out.elevation = in_Color.a;

                        vec2 dirXy = in_Position.xy - u_domeCenter.xy;
                        float lenDir = length(dirXy);
                        float lenSun = length(u_lightDirection.xy);
                        if (lenDir > 0.001 && lenSun > 0.001) {
                            vs_out.sunFactor = max(0.0, dot(dirXy / lenDir, u_lightDirection.xy / lenSun));
                        } else {
                            vs_out.sunFactor = 0.0;
                        }
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    uniform sampler2D u_texture0;
                    uniform sampler2D u_texture1;
                    uniform vec4 u_skyColor;
                    uniform vec4 u_cloudShadow; // rgb = ambient shadow tint, a = shadow strength
                    uniform float u_horizonCloudFade;

                    in VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoord1;
                        vec3 color;
                        float elevation;
                        float sunFactor;
                    } fs_in;

                    layout(location = 0) out vec4 out_FragColor;

                    void main() {
                        // Cross-layer domain warping & advection:
                        // Upper and lower cloud layers perturb each other's UV space,
                        // simulating convective billowing, curling, and fluid morphing.
                        vec2 warpCoord0 = fs_in.texCoord1 * 0.45 + vec2(0.13, 0.27);
                        float warp0X = texture(u_texture1, warpCoord0).r;
                        float warp0Y = texture(u_texture1, warpCoord0 + vec2(0.35, 0.65)).r;
                        vec2 warp0 = (vec2(warp0X, warp0Y) - 0.5) * 0.055;
                        vec4 tex0 = texture(u_texture0, fs_in.texCoord0 + warp0);

                        vec2 warpCoord1 = fs_in.texCoord0 * 0.45 + vec2(0.41, 0.79);
                        float warp1X = texture(u_texture0, warpCoord1).r;
                        float warp1Y = texture(u_texture0, warpCoord1 + vec2(0.28, 0.52)).r;
                        vec2 warp1 = (vec2(warp1X, warp1Y) - 0.5) * 0.035;
                        vec4 tex1 = texture(u_texture1, fs_in.texCoord1 + warp1);

                        // Pre-cloud atmospheric haze with smooth exponential decay:
                        // Applied strictly to the background sky gradient before clouds are composited,
                        // so clouds never get discolored or darkened by cold fog.
                        float skirtFactor = exp(-fs_in.elevation * 30.0);
                        float altitudeAttenuation = u_fogMode >= 0
                                ? (1.0 - smoothstep(20.0, 85.0, u_cameraHeight))
                                : 0.0;
                        float fogBlend = skirtFactor * altitudeAttenuation * 0.30;
                        vec3 skyBg = mix(fs_in.color, u_fogColor.rgb, fogBlend);

                        // Sunward cloud rim highlight: subtle 20% luminance boost when aligned toward the sun azimuth
                        vec3 sunCloudColor = u_skyColor.rgb * (1.0 + 0.20 * fs_in.sunFactor);

                        // Horizon cloud density attenuation:
                        // Due to spherical UV compression near the horizon, clouds crowd together.
                        // Smoothly thinning cloud density toward the horizon clears the horizon band.
                        float horizonFade = smoothstep(0.0, u_horizonCloudFade, fs_in.elevation);
                        float cloud0 = tex0.r * horizonFade;
                        float cloud1 = tex1.r * horizonFade;

                        // Dual-layer parallax & self-occlusion: upper cloud layer casts an ambient shadow onto the lower layer
                        vec3 lowerCloudTint = sunCloudColor * mix(vec3(1.0), u_cloudShadow.rgb, cloud1 * u_cloudShadow.a);
                        vec3 color0 = mix(skyBg, lowerCloudTint, cloud0);
                        vec3 color1 = mix(color0, sunCloudColor, cloud1);

                        out_FragColor = vec4(color1, 1.0);
                    }
                    """;

    final int locModelViewMatrix;
    final int locTexture0;
    final int locTexture1;
    final int locOuterOffset;
    final int locInnerOffset;
    final int locSkyColor;
    final int locDomeCenter;
    final int locCloudShadow;
    final int locHorizonCloudFade;

    SkyShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
        locModelViewMatrix = getUniformLocation(Uniforms.MODEL_VIEW_MATRIX);
        locTexture0 = getUniformLocation(Uniforms.TEXTURE_0);
        locTexture1 = getUniformLocation(Uniforms.TEXTURE_1);
        locOuterOffset = getUniformLocation(Uniforms.OUTER_OFFSET);
        locInnerOffset = getUniformLocation(Uniforms.INNER_OFFSET);
        locSkyColor = getUniformLocation(Uniforms.SKY_COLOR);
        locDomeCenter = getUniformLocation(Uniforms.DOME_CENTER);
        locCloudShadow = getUniformLocation(Uniforms.CLOUD_SHADOW);
        locHorizonCloudFade = getUniformLocation(Uniforms.HORIZON_CLOUD_FADE);
    }
}
