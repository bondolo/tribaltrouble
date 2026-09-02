package com.oddlabs.tt.engine.render.state;

import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.vbo.VBO;
import org.jspecify.annotations.Nullable;

public interface RenderContext {
    // State Management
    void setBlendMode(BlendMode mode);

    void setDepthMode(DepthMode mode);

    void setCullMode(CullMode mode);

    // Alpha to Coverage
    void setSampleAlphaToCoverage(boolean enabled);

    // Low-level state
    void setDepthTest(boolean enabled);

    void setDepthMask(boolean enabled);

    void setBlend(boolean enabled);

    void setCullFace(boolean enabled);

    void setCullFaceMode(int mode);

    // Depth Func
    void setDepthFunc(int func);

    // Color Mask
    void setColorMask(boolean r, boolean g, boolean b, boolean a);

    // Texture Management
    void setActiveTexture(int unit);

    void setTexture(int unit, int textureHandle);

    void setTexture(int unit, int textureHandle, int target);

    default void setTexture(int unit, @Nullable Texture texture) {
        if (texture != null) {
            setTexture(unit, texture.getHandle(), texture.getTarget());
        } else {
            setTexture(unit, 0);
        }
    }

    // Viewport
    void setViewport(int x, int y, int w, int h);

    int getViewportWidth();

    int getViewportHeight();

    // VAO
    void bindVertexArray(int vao);

    // VBO / FBO
    void bindBuffer(int target, int buffer);

    void bindFramebuffer(int target, int framebuffer);

    void invalidateTexture(int handle);

    void invalidateBuffer(int handle);

    void invalidateFramebuffer(int handle);

    void invalidateVertexArray(int handle);

    default void bindBuffer(int target, @Nullable VBO vbo) {
        bindBuffer(target, vbo != null ? vbo.getHandle() : 0);
    }

    // Clearing
    void clearColor(float r, float g, float b, float a);

    void clear(boolean color, boolean depth);

    // Scoped State (Try-with-resources)
    // These return a Closeable that restores the PREVIOUS state.
    ScopedState withBlendMode(BlendMode mode);

    ScopedState withDepthMode(DepthMode mode);

    ScopedState withCullMode(CullMode mode);

    ScopedState withSampleAlphaToCoverage(boolean enabled);

    ScopedState withColorMask(boolean r, boolean g, boolean b, boolean a);

    ScopedState withDepthFunc(int func);

    ScopedState withDrawBuffers(boolean mask);

    ScopedState withFramebufferSrgb(boolean enabled);

    // UBO Management
    void updateGlobalState(java.nio.ByteBuffer data);

    // Custom State
    void setBlendFunc(int src, int dst);

    void setBlendEquation(int equation);

    /**
     * Resets the blend functions for all draw buffers to the current global setBlendFunc state.
     * This is useful to clear per-buffer blend states (e.g. from glBlendFunci) after complex composite operations.
     */
    void resetBlendFunc();

    void setDrawBuffers(boolean mask);

    void setDrawBuffers(int[] attachments);

    void setFramebufferSrgb(boolean enabled);

    // Lifecycle & Debug
    void init();

    void applyDefaults(boolean enableMultisample);

    /**
     * Verifies that the tracked state matches the actual OpenGL state.
     *
     * @throws IllegalStateException if a mismatch is found.
     */
    void validate();
}
