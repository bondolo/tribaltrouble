package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * A shader for rendering particles using hardware instancing.
 * Billboard expansion is performed in the vertex shader using gl_VertexID.
 */
public final class ParticleShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String TEXTURE_0 = "u_texture0";
        String IS_ADDITIVE = "u_isAdditive";
    }

    public interface Attributes {
        String CENTER_POSITION = "in_CenterPosition";
        String SIZE = "in_Size";
        String COLOR = Shader.COLOR;
        String UV_COORDS_1 = "in_UvCoords1"; // u1, v1, u2, v2
        String UV_COORDS_2 = "in_UvCoords2"; // u3, v3, u4, v4
    }

    public enum Attribute implements VertexAttribute {
        CENTER_POSITION(Attributes.CENTER_POSITION, 3, GL11.GL_FLOAT),
        SIZE(Attributes.SIZE, 3, GL11.GL_FLOAT), // radius_x, radius_y, radius_z
        COLOR(Attributes.COLOR, 4, GL11.GL_FLOAT),
        UV_COORDS_1(Attributes.UV_COORDS_1, 4, GL11.GL_FLOAT),
        UV_COORDS_2(Attributes.UV_COORDS_2, 4, GL11.GL_FLOAT);

        private final @NonNull String name;
        private final int componentCount;
        private final int glType;
        private final boolean normalized;

        Attribute(@NonNull String name, int componentCount, int glType) {
            this(name, componentCount, glType, false);
        }

        Attribute(@NonNull String name, int componentCount, int glType, boolean normalized) {
            this.name = name;
            this.componentCount = componentCount;
            this.glType = glType;
            this.normalized = normalized;
        }

        @Override
        public @NonNull String getName() {
            return name;
        }

        @Override
        public int getComponentCount() {
            return componentCount;
        }

        @Override
        public int getGlType() {
            return glType;
        }

        @Override
        public boolean isNormalized() {
            return normalized;
        }
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ +
            GLOBAL_STATE_BLOCK +
            """
            
            layout(location = 0) in vec3 in_CenterPosition;
            layout(location = 1) in vec3 in_Size;
            layout(location = 2) in vec4 in_Color;
            layout(location = 3) in vec4 in_UvCoords1;
            layout(location = 4) in vec4 in_UvCoords2;
            
            uniform mat4 u_modelViewMatrix;
            
            out vec2 v_texCoord;
            out vec4 v_color;
            out float v_fogDist;
            
            void main() {
                vec3 center = in_CenterPosition;
                vec3 radius = in_Size;
                v_color = in_Color;
            
                mat4 mv = u_modelViewMatrix;
                vec3 right = vec3(mv[0][0], mv[1][0], mv[2][0]);
                vec3 up = vec3(mv[0][1], mv[1][1], mv[2][1]);
            
                vec3 r_plus_up = (right + up) * radius;
                vec3 r_minus_up = (right - up) * radius;
            
                vec4 viewCenter = mv * vec4(center, 1.0);
                v_fogDist = length(viewCenter.xyz);

                vec3 p;
                if (gl_VertexID == 0) {
                    // Bottom-left
                    p = center - r_plus_up;
                    v_texCoord = in_UvCoords1.xy;
                } else if (gl_VertexID == 1) {
                    // Bottom-right
                    p = center + r_minus_up;
                    v_texCoord = in_UvCoords1.zw;
                } else if (gl_VertexID == 2) {
                    // Top-left
                    p = center - r_minus_up;
                    v_texCoord = in_UvCoords2.zw;
                } else {
                    // Top-right
                    p = center + r_plus_up;
                    v_texCoord = in_UvCoords2.xy;
                }
                
                gl_Position = u_projectionMatrix * (mv * vec4(p, 1.0));
            }
            """;

    private static final String FRAGMENT_SHADER =
            """
                    #version 410 core
                    """ +
                    GLOBAL_STATE_BLOCK +
                    FOG_FUNCTION +
                    """
                            uniform sampler2D u_texture0;
                            uniform float u_isAdditive;
                            
                            in vec2 v_texCoord;
                            in vec4 v_color;
                            in float v_fogDist;
                            
                            layout(location = 0) out vec4 out_FragColor;
                            
                            void main() {
                                vec4 texColor = texture(u_texture0, v_texCoord);
                                vec4 finalColor = v_color * texColor;
                            
                                if (finalColor.a <= 0.0) {
                                    discard;
                                }
                            
                                float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                                vec3 foggedColor = mix(u_fogColor.rgb, finalColor.rgb, fogFactor);
                                if (u_isAdditive > 0.5) {
                                    foggedColor = finalColor.rgb * fogFactor;
                                }
                                out_FragColor = vec4(foggedColor, finalColor.a);
                            }
                            """;

    public ParticleShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}
