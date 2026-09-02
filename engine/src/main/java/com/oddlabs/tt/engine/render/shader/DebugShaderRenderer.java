package com.oddlabs.tt.engine.render.shader;

import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import org.lwjgl.opengl.GL11;

/**
 * Renders debug graphics
 */
public final class DebugShaderRenderer extends ShaderRenderer {

    private float pointSize = 1.0f;

    public DebugShaderRenderer(ShaderProgram shader, MatrixStack modelViewStack, MatrixStack projectionStack) {
        super(shader, modelViewStack, projectionStack);
    }

    public void setPointSize(float size) {
        this.pointSize = size;
    }

    /**
     * Begins a new drawing sequence.
     *
     * @param glMode The OpenGL primitive mode (e.g., GL11.GL_LINES, GL11.GL_TRIANGLES).
     */
    @Override
    public void begin(int glMode) {
        // State is managed in the flush() or render call now.
        super.begin(glMode);
    }

    /**
     * Adds a vertex to the buffer with specified position and color.
     */
    public void vertex(float x, float y, float z, float r, float g, float b) {
        super.vertex(x, y, z, 0, 0, 1, r, g, b, 1, 0, 0);
    }

    /**
     * Adds a vertex to the buffer with specified position and color.
     */
    public void vertex(float x, float y, float z, float r, float g, float b, float a) {
        super.vertex(x, y, z, 0, 0, 1, r, g, b, a, 0, 0);
    }

    /**
     * Adds a vertex to the buffer with specified position and color.
     *
     * @param color A float array containing the RGB or RGBA color components.
     */
    public void vertex(float x, float y, float z, float[] color) {
        vertex(x, y, z, color[0], color[1], color[2], color.length >= 4 ? color[3] : 1);
    }

    /**
     * Ends the drawing sequence and flushes any remaining vertices.
     */
    @Override
    public void end() {
        var context = RenderContext.current();
        try (var _ = context.withDepthMode(DepthMode.NONE); var _ = context.withBlendMode(
                BlendMode.ALPHA)) {
            flush(pointSize);
        }
    }

    public void drawAxes(float center, float z, float[] xAxisColor, float[] yAxisColor,
            float[] zAxisColor) {
        begin(GL11.GL_LINES);
        try {
            // X axis - red
            vertex(center, center, z, xAxisColor);
            vertex(center + 10, center, z, xAxisColor);

            // Y axis - green
            vertex(center, center, z, yAxisColor);
            vertex(center, center + 10, z, yAxisColor);

            // Z axis - blue
            vertex(center, center, z, zAxisColor);
            vertex(center, center, z + 10, zAxisColor);
        } finally {
            end();
        }
    }
}
