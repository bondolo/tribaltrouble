package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.settings.AccessibilitySettings;
import org.jspecify.annotations.Nullable;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.CullMode;
import com.oddlabs.tt.engine.render.state.DepthMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.vbo.FloatVBO;
import com.oddlabs.tt.engine.vbo.VertexArray;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
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
        } finally {
            // Unbind textures to prevent feedback loops in next frame
            context.setTexture(0, 0);
            context.setTexture(1, 0);
        }

        // 3. Render GUI directly onto the default framebuffer on top of the composited 3D scene.
        // This guarantees UI text and vector borders remain 1:1 pixel-sharp, completely isolated from
        // unsharp masking, contrast S-curves, smart inversion, and team outlines.
        try (var _ = context.withBlendMode(BlendMode.PREMULTIPLIED)) {
            guiRenderCallback.accept(context);
        } finally {
            context.resetBlendFunc();
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
        quadVBO.close();
    }
}
