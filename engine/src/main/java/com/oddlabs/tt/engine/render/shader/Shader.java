package com.oddlabs.tt.engine.render.shader;

import com.oddlabs.util.Color;
import org.joml.Matrix4fc;

/**
 * Base interface for OpenGL shaders.
 */
public interface Shader {
    // Standard Global Header
    String SHADER_HEADER = """
            #version 410 core
            precision highp float;
            precision highp int;
            """;

    interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
    }

    interface Attributes {
        String POSITION = "in_Position";
        String NORMAL = "in_Normal";
        String TEX_COORD = "in_TexCoord";
        String COLOR = "in_Color";
    }

    String GLOBAL_STATE_BLOCK = """
            layout(std140) uniform GlobalState {
                mat4 u_projectionMatrix;
                mat4 u_viewMatrix;
                vec4 u_fogColor;
                vec4 u_fogParams;
                float u_cameraHeight;
                float u_fogHeightFactor;
                float u_globalTime;
                int u_fogMode;

                // --- Lighting Params ---
                vec4 u_lightDirection;         // xyz = world direction, w = sun intensity
                vec4 u_globalAmbient;          // rgb = sky ambient color, a = unused
                vec4 u_groundAmbient;          // rgb = ground ambient color, a = unused
                vec4 u_sunColor;               // rgb = sun color, a = unused

                // --- Water Params ---
                vec4 u_waveDirLength[3];       // xy = dir, z = length
                vec4 u_waveAmpSteep[3];        // x = amp, y = steepness
                vec4 u_scrollOffsets;          // xy = offset0, zw = offset1
                float u_waveTime;
                float u_waterRepeatRate;
                float u_waterDetailRepeatRate;
                float _pad2;
            };
            """;

    boolean inUse();

    int getAttributeLocation(String name);

    int getUniformLocation(String name);

    void setUniform(String name, int[] values);

    void setUniform(int location, int[] values);

    void setUniform(String name, int value);

    void setUniform(int location, int value);

    void setUniform(String name, float value);

    void setUniform(int location, float value);

    void setUniform(String name, boolean value);

    void setUniform(int location, boolean value);

    void setUniform(String name, float x, float y);

    void setUniform(int location, float x, float y);

    void setUniform(String name, float x, float y, float z);

    void setUniform(int location, float x, float y, float z);

    /**
     * set the named uniform to the specified color.
     *
     * @param color should be in linear space but is converted if necessary
     */
    void setUniform(String name, Color color);

    void setUniform(int location, Color color);

    void setUniform(String name, Matrix4fc matrix);

    void setUniform(int location, Matrix4fc matrix);

    void setUniform(String name, boolean transpose, Matrix4fc matrix);

    void setUniform(int location, boolean transpose, Matrix4fc matrix);
}
