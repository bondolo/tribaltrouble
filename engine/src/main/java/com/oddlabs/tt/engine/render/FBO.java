package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.base.resource.NativeResource;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/**
 * Wraps a native Framebuffer Object
 */
public final class FBO extends NativeResource<FBO.Buffer> {

    static final class Buffer extends NativeResource.NativeState {

        private final int handle;

        Buffer() {
            this.handle = GL30.glGenFramebuffers();
        }

        @Override
        public void close() {
            RenderContext.current().invalidateFramebuffer(handle);
            GL30.glDeleteFramebuffers(handle);
        }
    }

    private int width;
    private int height;
    private int samples;
    private int colorRbo;
    private int maskRbo;
    private int depthRbo;
    private @Nullable Texture colorTexture;
    private @Nullable Texture maskTexture;
    private @Nullable Texture depthTexture;

    public FBO(int width, int height) {
        super(new Buffer());
        this.width = width;
        this.height = height;
    }

    public static FBO createSceneFBO(int width, int height) {
        FBO fbo = new FBO(width, height);
        fbo.bind();

        // HDR Color Texture (Float16 for high dynamic range)
        Texture color = new Texture(width, height, GL30.GL_RGBA16F, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE);
        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT0, color);
        fbo.colorTexture = color;

        // Mask Texture (Standard RGBA for team color/stencil)
        Texture mask = new Texture(width, height, GL11.GL_RGBA8, GL11.GL_NEAREST, GL11.GL_NEAREST,
                GL12.GL_CLAMP_TO_EDGE);
        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT1, mask);
        fbo.maskTexture = mask;

        // Depth Texture (24-bit depth)
        Texture depth = new Texture(width, height, GL30.GL_DEPTH_COMPONENT24, GL11.GL_NEAREST, GL11.GL_NEAREST,
                GL12.GL_CLAMP_TO_EDGE);
        fbo.attachTexture(GL30.GL_DEPTH_ATTACHMENT, depth);
        fbo.depthTexture = depth;

        // Explicitly declare draw buffers
        RenderContext.current().setDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_COLOR_ATTACHMENT1});

        fbo.checkStatus();
        fbo.unbind();
        return fbo;
    }

    public static FBO createMultisampleSceneFBO(int width, int height, int requestedSamples) {
        FBO fbo = new FBO(width, height);
        int maxSamples = GL11.glGetInteger(GL30.GL_MAX_SAMPLES);
        fbo.samples = Math.max(1, Math.min(requestedSamples, maxSamples));
        fbo.bind();

        // HDR Color RBO (Float16 for high dynamic range)
        fbo.colorRbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, fbo.colorRbo);
        GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, fbo.samples, GL30.GL_RGBA16F, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER,
                fbo.colorRbo);

        // Mask RBO (Standard RGBA8 for team color/stencil)
        fbo.maskRbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, fbo.maskRbo);
        GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, fbo.samples, GL11.GL_RGBA8, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_RENDERBUFFER,
                fbo.maskRbo);

        // Depth RBO (24-bit depth)
        fbo.depthRbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, fbo.depthRbo);
        GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, fbo.samples, GL30.GL_DEPTH_COMPONENT24, width,
                height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER,
                fbo.depthRbo);

        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);

        // Explicitly declare draw buffers
        RenderContext.current().setDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});

        fbo.checkStatus();
        fbo.unbind();
        return fbo;
    }

    public void resize(int width, int height) {
        if (this.width == width && this.height == height) return;
        this.width = width;
        this.height = height;

        if (samples > 1) {
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, colorRbo);
            GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, samples, GL30.GL_RGBA16F, width, height);

            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, maskRbo);
            GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, samples, GL11.GL_RGBA8, width, height);

            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRbo);
            GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, samples, GL30.GL_DEPTH_COMPONENT24, width,
                    height);

            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);

            bind();
            RenderContext.current().setDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
            checkStatus();
            unbind();
            return;
        }

        if (colorTexture != null) {
            colorTexture.close();
            // Re-create color texture with new dimensions
            colorTexture = new Texture(width, height, GL30.GL_RGBA16F, GL11.GL_LINEAR, GL11.GL_LINEAR,
                    GL12.GL_CLAMP_TO_EDGE);
        }
        if (maskTexture != null) {
            maskTexture.close();
            maskTexture = new Texture(width, height, GL11.GL_RGBA8, GL11.GL_NEAREST, GL11.GL_NEAREST,
                    GL12.GL_CLAMP_TO_EDGE);
        }
        if (depthTexture != null) {
            depthTexture.close();
            // Re-create depth texture with new dimensions
            depthTexture = new Texture(width, height, GL30.GL_DEPTH_COMPONENT24, GL11.GL_NEAREST, GL11.GL_NEAREST,
                    GL12.GL_CLAMP_TO_EDGE);
        }

        bind();
        if (colorTexture != null) attachTexture(GL30.GL_COLOR_ATTACHMENT0, colorTexture);
        if (maskTexture != null) attachTexture(GL30.GL_COLOR_ATTACHMENT1, maskTexture);
        if (depthTexture != null) attachTexture(GL30.GL_DEPTH_ATTACHMENT, depthTexture);

        // Restore draw buffers state after resize/rebind (if we have color attachments)
        RenderContext context = RenderContext.current();
        if (colorTexture != null || maskTexture != null) {
            context.setDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
        } else {
            context.setDrawBuffers(new int[]{GL11.GL_NONE});
            GL11.glReadBuffer(GL11.GL_NONE);
        }

        checkStatus();
        unbind();
    }

    public void bind() {
        RenderContext context = RenderContext.current();
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, getHandle());
        context.setViewport(0, 0, width, height);
    }

    public void unbind() {
        RenderContext.current().bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void detachAll() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, getHandle());
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, 0, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, 0, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
        colorTexture = null;
        maskTexture = null;
        depthTexture = null;
    }

    public @Nullable Texture getColorTexture() {
        return colorTexture;
    }

    public @Nullable Texture getMaskTexture() {
        return maskTexture;
    }

    public @Nullable Texture getDepthTexture() {
        return depthTexture;
    }

    public void blitDepthTo(FBO target) {
        RenderContext context = RenderContext.current();
        context.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, getHandle());
        context.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.getHandle());
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, target.width, target.height,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        // Bind back to original target
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, getHandle());
    }

    public void attachTexture(int attachmentPoint, Texture texture) {
        attachTexture(attachmentPoint, texture, 0);
    }

    public void attachTexture(int attachmentPoint, Texture texture, int level) {
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachmentPoint, GL11.GL_TEXTURE_2D, texture.getHandle(),
                level);
        if (attachmentPoint == GL30.GL_COLOR_ATTACHMENT0) colorTexture = texture;
        else if (attachmentPoint == GL30.GL_COLOR_ATTACHMENT1) maskTexture = texture;
        else if (attachmentPoint == GL30.GL_DEPTH_ATTACHMENT) depthTexture = texture;
    }

    public void checkStatus() {
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer is incomplete: " + status);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getHandle() {
        return state.handle;
    }

    public void resolveTo(FBO target) {
        RenderContext context = RenderContext.current();
        context.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, getHandle());
        context.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.getHandle());

        // Resolve Color Attachment 0 (HDR Scene Color)
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, target.width, target.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        // Resolve Color Attachment 1 (Mask Buffer)
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT1);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT1);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, target.width, target.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        // Restore target draw buffers and unbind
        target.bind();
        context.setDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
        target.unbind();
    }

    @Override
    public void close() {
        super.close();

        if (colorRbo != 0) {
            GL30.glDeleteRenderbuffers(colorRbo);
            colorRbo = 0;
        }
        if (maskRbo != 0) {
            GL30.glDeleteRenderbuffers(maskRbo);
            maskRbo = 0;
        }
        if (depthRbo != 0) {
            GL30.glDeleteRenderbuffers(depthRbo);
            depthRbo = 0;
        }

        if (colorTexture != null) {
            colorTexture.close();
            colorTexture = null;
        }
        if (maskTexture != null) {
            maskTexture.close();
            maskTexture = null;
        }
        if (depthTexture != null) {
            depthTexture.close();
            depthTexture = null;
        }
    }
}
