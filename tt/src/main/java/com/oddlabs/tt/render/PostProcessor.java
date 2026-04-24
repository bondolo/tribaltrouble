package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.render.shader.PostProcessShader;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

import java.nio.FloatBuffer;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Manages the full-screen post-processing pipeline.
 * Handles rendering the scene to an FBO and applying effects via PostProcessShader.
 */
public final class PostProcessor implements AutoCloseable {
    private final @NonNull PostProcessShader shader;
    private final @NonNull VertexArray vao;
    private final @NonNull FloatVBO quadVBO;
    private final @NonNull FBO sceneFBO;
    private final @NonNull FBO depthCopyFBO;
    private int currentWidth;
    private int currentHeight;

    public PostProcessor(int width, int height) {
        this.currentWidth = width;
        this.currentHeight = height;
        this.shader = new PostProcessShader();
        this.sceneFBO = FBO.createSceneFBO(width, height);
        
        // Depth Copy FBO (for Soft Particles)
        this.depthCopyFBO = new FBO(width, height);
        this.depthCopyFBO.bind();
        Texture depthCopy = new Texture(width, height, GL30.GL_DEPTH_COMPONENT24, GL11.GL_NEAREST, GL11.GL_NEAREST, GL12.GL_CLAMP_TO_EDGE);
        this.depthCopyFBO.attachTexture(GL30.GL_DEPTH_ATTACHMENT, depthCopy);
        // This FBO has no color attachment
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        this.depthCopyFBO.checkStatus();
        this.depthCopyFBO.unbind();

        // Setup Full Screen Quad
        this.vao = new VertexArray();
        this.vao.bind();

        float[] quadVertices = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f
        };
        FloatBuffer buffer = BufferUtils.createFloatBuffer(quadVertices.length).put(quadVertices).flip();
        this.quadVBO = new FloatVBO(GL15.GL_STATIC_DRAW, buffer);

        int posLoc = shader.getAttributeLocation(PostProcessShader.Attributes.POSITION);
        if (posLoc >= 0) {
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(posLoc);
            quadVBO.vertexAttribPointer(posLoc, 2, 0, 0);
        }

        this.vao.unbind();
    }

    public boolean resize(int width, int height) {
        if (this.currentWidth == width && this.currentHeight == height) return false;
        this.currentWidth = width;
        this.currentHeight = height;
        sceneFBO.resize(width, height);
        
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
        sceneFBO.blitDepthTo(depthCopyFBO);
    }

    public @NonNull Texture getDepthCopyTexture() {
        return Objects.requireNonNull(depthCopyFBO.getDepthTexture());
    }

    public void bindSceneFBO(@NonNull RenderContext context) {
        sceneFBO.bind(context);
    }

    public void bindSceneFBO() {
        sceneFBO.bind();
    }

    public void unbindSceneFBO(@NonNull RenderContext context) {
        sceneFBO.unbind(); // unbind already uses Renderer context
    }

    public void unbindSceneFBO() {
        sceneFBO.unbind();
    }

    public void renderComposite(@NonNull RenderContext context, @NonNull Consumer<@NonNull RenderContext> guiRenderCallback) {
        // 1. Render GUI into the Scene FBO (on top of the 3D scene)
        bindSceneFBO(context);

        // Use glBlendFunci to set different blend modes for different draw buffers.
        // Buffer 0 (Color): GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
        // Buffer 1 (Mask): GL_ONE, GL_ZERO (Overwrite)
        GL40.glBlendFunci(0, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL40.glBlendFunci(1, GL11.GL_ONE, GL11.GL_ZERO);

        guiRenderCallback.accept(context);

        // Restore global blend state across all buffers
        context.resetBlendFunc();

        unbindSceneFBO(context);

        // 2. Composite the FBO to the screen with Post-Processing (CVD, High Contrast, Team Stencil)
        // Render to default framebuffer (screen)
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        context.setViewport(0, 0, currentWidth, currentHeight);
        context.setDrawBuffers(false); // Ensure only back buffer is active for FBO 0
        context.clear(true, true);

        try (var _ = shader.use();
             var _ = context.withDepthMode(DepthMode.NONE);
             var _ = context.withCullMode(CullMode.NONE)) {

            Settings settings = Settings.getSettings();
            shader.setUniform(PostProcessShader.Uniforms.CVD_MODE, settings.cvd_mode);
            shader.setUniform(PostProcessShader.Uniforms.CVD_INTENSITY, settings.cvd_intensity);
            shader.setUniform(PostProcessShader.Uniforms.HIGH_CONTRAST, settings.high_contrast);
            shader.setUniform(PostProcessShader.Uniforms.CONTRAST_INTENSITY, settings.contrast_intensity);
            shader.setUniform(PostProcessShader.Uniforms.TEAM_STENCIL, settings.team_stencil);
            shader.setUniform(PostProcessShader.Uniforms.SCENE_TEXTURE, 0);
            shader.setUniform(PostProcessShader.Uniforms.MASK_TEXTURE, 1);

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
        depthCopyFBO.close();
        vao.close();
        quadVBO.close();
    }
}
