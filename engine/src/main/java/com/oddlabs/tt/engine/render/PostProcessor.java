package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.render.state.ScopedState;
import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import com.oddlabs.tt.engine.vbo.VertexArray;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Coordinates the full-screen post-processing pipeline, rendering the scene to an FBO and applying
 * accessibility and visual filters via PostProcessShader.
 */
public final class PostProcessor implements AutoCloseable {
    private final PostProcessShader shader;
    private final VertexArray vao;
    private final FBO sceneFBO;
    private final int samples;
    private @Nullable FBO msaaSceneFBO;
    private final FBO depthCopyFBO;
    private final AccessibilitySettings accessibility;
    private int currentWidth;
    private int currentHeight;

    public PostProcessor(AccessibilitySettings accessibility, int width, int height, int samples) {
        this.accessibility = accessibility;
        this.currentWidth = width;
        this.currentHeight = height;
        this.shader = new PostProcessShader();
        try (var _ = shader.use()) {
            shader.updateDimensions(width, height);
        }
        this.sceneFBO = FBO.createSceneFBO(width, height);
        this.samples = samples;
        this.msaaSceneFBO = null;

        // Depth Copy FBO (for Soft Particles)
        this.depthCopyFBO = FBO.createDepthOnlyFBO(width, height);

        // Setup Full-Screen VAO (empty VAO for procedural gl_VertexID triangle rendering)
        this.vao = new VertexArray();
    }

    private FBO getActiveSceneFBO() {
        if (samples > 1 && RenderContext.current().isMultisampleEnabled()) {
            if (msaaSceneFBO == null) {
                msaaSceneFBO = FBO.createMultisampleSceneFBO(currentWidth, currentHeight, samples);
            }
            return msaaSceneFBO;
        }
        if (msaaSceneFBO != null) {
            msaaSceneFBO.close();
            msaaSceneFBO = null;
        }
        return sceneFBO;
    }

    /**
     * Resizes internal framebuffers and updates shader viewport dimensions.
     *
     * @param width the new width in pixels
     * @param height the new height in pixels
     * @return {@code true} if dimensions changed; {@code false} otherwise
     */
    public boolean resize(int width, int height) {
        if (this.currentWidth == width && this.currentHeight == height) return false;
        this.currentWidth = width;
        this.currentHeight = height;
        try (var _ = shader.use()) {
            shader.updateDimensions(width, height);
        }
        sceneFBO.resize(width, height);
        if (!RenderContext.current().isMultisampleEnabled() && msaaSceneFBO != null) {
            msaaSceneFBO.close();
            msaaSceneFBO = null;
        } else if (msaaSceneFBO != null) {
            msaaSceneFBO.resize(width, height);
        }

        depthCopyFBO.resize(width, height);
        return true;
    }

    public void copyDepthBuffer() {
        getActiveSceneFBO().blitDepthTo(depthCopyFBO);
    }

    public Texture getDepthCopyTexture() {
        return depthCopyFBO.getDepthTexture();
    }

    /**
     * Binds the active scene framebuffer for 3D rendering and returns a scoped handle that unbinds
     * back to the default framebuffer upon exit.
     *
     * @param width the viewport width in pixels
     * @param height the viewport height in pixels
     * @return a scoped handle restoring framebuffer 0 upon closure
     */
    public ScopedState withScene(int width, int height) {
        resize(width, height);
        getActiveSceneFBO().bind();
        return () -> RenderContext.current().bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    /**
     * Resolves multisample buffers if enabled and composites post-processing effects onto the default framebuffer.
     *
     * @param context the active render context
     */
    public void renderComposite(RenderContext context) {
        FBO activeSceneFBO = getActiveSceneFBO();

        // 1. If MSAA was used for 3D rendering, resolve activeSceneFBO to sceneFBO
        if (activeSceneFBO != sceneFBO) {
            activeSceneFBO.resolveTo(sceneFBO);
        }

        // 2. Composite the 3D scene from sceneFBO to the default framebuffer (screen) with Post-Processing
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        context.setViewport(0, 0, currentWidth, currentHeight);
        context.setDrawBuffers(false); // Ensure only back buffer is active for FBO 0
        context.clear(true, true);

        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.NONE); var _ = context.withDepthMode(
                DepthMode.NONE); var _ = context.withCullMode(CullMode.NONE)) {

            shader.updateAccessibility(accessibility);

            context.setTexture(0, sceneFBO.getColorTexture());
            context.setTexture(1, sceneFBO.getMaskTexture());

            vao.bind();
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            vao.unbind();
        } finally {
            // Unbind textures to prevent feedback loops in next frame
            context.setTexture(0, 0);
            context.setTexture(1, 0);
        }
    }

    @Override
    public void close() {
        shader.close();
        sceneFBO.close();
        if (msaaSceneFBO != null) {
            msaaSceneFBO.close();
        }
        depthCopyFBO.close();
        vao.close();
    }
}
