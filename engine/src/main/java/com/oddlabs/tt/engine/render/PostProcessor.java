package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import org.jspecify.annotations.Nullable;
import com.oddlabs.tt.engine.render.shader.PostProcessShader;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import java.util.function.Consumer;

/**
 * Coordinates the full-screen post-processing pipeline, rendering the scene to an FBO and applying
 * accessibility and visual filters via PostProcessShader.
 */
public final class PostProcessor implements AutoCloseable {
    private final PostProcessShader shader;
    private final VertexArray vao;
    private final FloatVBO quadVBO;
    private final FBO sceneFBO;
    private final int samples;
    private @Nullable FBO msaaSceneFBO;
    private final FBO depthCopyFBO;
    private final AccessibilitySettings accessibility;
    private int currentWidth;
    private int currentHeight;

    public PostProcessor(AccessibilitySettings accessibility, int width, int height) {
        this(accessibility, width, height, 0);
    }

    public PostProcessor(AccessibilitySettings accessibility, int width, int height, int samples) {
        this.accessibility = accessibility;
        this.currentWidth = width;
        this.currentHeight = height;
        this.shader = new PostProcessShader();
        this.sceneFBO = FBO.createSceneFBO(width, height);
        this.samples = samples;
        this.msaaSceneFBO = null;

        // Depth Copy FBO (for Soft Particles)
        this.depthCopyFBO = new FBO(width, height);
        this.depthCopyFBO.bind();
        Texture depthCopy = new Texture(width, height, GL30.GL_DEPTH_COMPONENT24, GL11.GL_NEAREST,
                GL11.GL_NEAREST,
                GL12.GL_CLAMP_TO_EDGE);
        this.depthCopyFBO.attachTexture(GL30.GL_DEPTH_ATTACHMENT, depthCopy);
        // This FBO has no color attachment
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        this.depthCopyFBO.checkStatus();
        this.depthCopyFBO.unbind();

        // Setup Full-Screen Quad
        this.vao = new VertexArray();
        this.vao.bind();

        try (var stack = MemoryStack.stackPush()) {
            this.quadVBO = new FloatVBO(GL15.GL_STATIC_DRAW, stack.floats(
                    -1.0f, -1.0f,
                    1.0f, -1.0f,
                    -1.0f, 1.0f,
                    1.0f, 1.0f
            ));
        }

        GL20.glEnableVertexAttribArray(0);
        quadVBO.vertexAttribPointer(0, 2, 0, 0);

        this.vao.unbind();
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

    public boolean resize(int width, int height) {
        if (this.currentWidth == width && this.currentHeight == height) return false;
        this.currentWidth = width;
        this.currentHeight = height;
        sceneFBO.resize(width, height);
        if (!RenderContext.current().isMultisampleEnabled() && msaaSceneFBO != null) {
            msaaSceneFBO.close();
            msaaSceneFBO = null;
        } else if (msaaSceneFBO != null) {
            msaaSceneFBO.resize(width, height);
        }

        depthCopyFBO.resize(width, height);
        depthCopyFBO.bind();
        // Since resize() in FBO.java doesn't handle custom depth-only FBOs cleanly yet,
        // we'll manually ensure it's still color-less.
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        depthCopyFBO.unbind();

        return true;
    }

    public void copyDepthBuffer() {
        getActiveSceneFBO().blitDepthTo(depthCopyFBO);
    }

    public Texture getDepthCopyTexture() {
        return depthCopyFBO.getDepthTexture();
    }

    public void bindSceneFBO() {
        getActiveSceneFBO().bind();
    }

    public void unbindSceneFBO() {
        getActiveSceneFBO().unbind();
    }

    public void renderComposite(RenderContext context, Consumer<
            RenderContext> guiRenderCallback) {
        FBO activeSceneFBO = getActiveSceneFBO();

        // 1. If MSAA was used for 3D rendering, resolve activeSceneFBO to sceneFBO before GUI
        if (activeSceneFBO != sceneFBO) {
            activeSceneFBO.resolveTo(sceneFBO);
        }

        // 2. Render GUI directly into the single-sampled Scene FBO (on top of the resolved 3D scene)
        sceneFBO.bind();

        // Ensure blending is enabled for the GUI pass.
        // Buffer 0 (Color): GL_ONE, GL_ONE_MINUS_SRC_ALPHA (Premultiplied Linear)
        // Buffer 1 (Mask): Wipes unit color proportionally and uses MAX for the marker alpha.
        try (var _ = context.withBlendMode(BlendMode.CUSTOM)) {
            context.setBlend(true);
            context.setBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // Mask RGB: Wipe background unit color as GUI becomes opaque
            // Mask Alpha: Use MAX to prevent marker (0.5) from accumulating to 1.0
            GL40.glBlendEquationSeparatei(1, GL14.GL_FUNC_ADD, GL14.GL_MAX);
            GL40.glBlendFunci(1, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            try (var stack = MemoryStack.stackPush()) {
                GL20.glDrawBuffers(stack.ints(GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1));
            }

            guiRenderCallback.accept(context);

            // Explicitly reset per-buffer state to prevent leaking into next pass/frame
            GL40.glBlendEquationSeparatei(1, GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            try (var stack = MemoryStack.stackPush()) {
                GL20.glDrawBuffers(stack.ints(GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1));
            }
        }

        sceneFBO.unbind();

        // 3. Composite the FBO to the screen with Post-Processing (CVD, High Contrast, Team Stencil)
        // Render to the default framebuffer (screen)
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        context.setViewport(0, 0, currentWidth, currentHeight);
        context.setDrawBuffers(false); // Ensure only back buffer is active for FBO 0
        context.clear(true, true);

        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.NONE); var _ = context.withDepthMode(
                DepthMode.NONE); var _ = context.withCullMode(CullMode.NONE)) {

            shader.setAccessibilityModes(accessibility.cvd_mode, accessibility.high_contrast);

            shader.setUniform(shader.locCvdIntensity, accessibility.cvd_intensity);
            shader.setUniform(shader.locContrastIntensity, accessibility.contrast_intensity);
            shader.setUniform(shader.locInvertColors, accessibility.invert_colours);
            shader.setUniform(shader.locContrastBrightness, accessibility.contrast_brightness);
            shader.setUniform(shader.locContrastClarity, accessibility.contrast_clarity);
            shader.setUniform(shader.locTeamStencil, accessibility.team_stencil);
            shader.setUniform(shader.locSceneTexture, 0);
            shader.setUniform(shader.locMaskTexture, 1);

            context.setTexture(0, sceneFBO.getColorTexture());
            context.setTexture(1, sceneFBO.getMaskTexture());

            vao.bind();
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
            vao.unbind();
        }

        // Unbind textures to prevent feedback loops in next frame
        context.setTexture(0, 0);
        context.setTexture(1, 0);
        context.setTexture(2, 0);
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
        quadVBO.close();
    }
}
