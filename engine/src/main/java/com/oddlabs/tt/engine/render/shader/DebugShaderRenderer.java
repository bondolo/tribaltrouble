package com.oddlabs.tt.engine.render.shader;

import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.util.Color;
import org.joml.Matrix4fc;

/**
 * Renders debug graphics.
 */
public final class DebugShaderRenderer extends ShaderRenderer {

    private float pointSize = 1.0f;

    public DebugShaderRenderer() {
        this(new DebugMeshShader());
    }

    DebugShaderRenderer(DebugMeshShader shader) {
        super(shader);
    }

    public void setPointSize(float size) {
        this.pointSize = size;
    }

    /**
     * Begins a new drawing sequence.
     *
     * @param glMode The OpenGL primitive mode (e.g., GL11.GL_LINES, GL11.GL_POINTS).
     * @param modelView The view matrix for transformation.
     */
    @Override
    public void begin(int glMode, Matrix4fc modelView) {
        super.begin(glMode, modelView);
    }

    /**
     * Adds a vertex to the buffer with specified position and color.
     */
    public void vertex(float x, float y, float z, Color color) {
        super.vertex(x, y, z, 0, 0, 1, color.r(), color.g(), color.b(), color.a(), 0, 0);
    }

    /**
     * Adds a vertex to the buffer with specified position and color.
     */
    public void vertex(float x, float y, float z, float r, float g, float b) {
        super.vertex(x, y, z, 0, 0, 1, r, g, b, 1, 0, 0);
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
}
