package com.oddlabs.tt.gui;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.FrameDriver;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.render.state.ScopedState;
import com.oddlabs.tt.gui.render.UIRenderer;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Container for the 2D user interface
 */
public final class GUI implements Animated, FrameDriver {
    private final @NonNull LocalInput localInput;
    private final GUIRenderer guiRenderer = new GUIRenderer();
    private @NonNull GUIRoot current_root;
    private @Nullable Fade fade;
    private @Nullable UIRenderer renderer;
    private final CameraState frustum_state = new CameraState();
    private @Nullable Runnable closeHandler;

    public GUI(@NonNull LocalInput localInput) {
        this.localInput = localInput;
        this.current_root = createRoot();
    }

    public GUI() {
        this(new LocalInput(Renderer.getRenderer().getWindow()));
    }

    public @NonNull LocalInput getLocalInput() {
        return localInput;
    }

    public void setCloseHandler(@Nullable Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }

    @Override
    public void tick(@NonNull NetworkSelector network) {
        localInput.poll(getGUIRoot());
    }

    @Override
    public void onCloseRequested() {
        if (closeHandler != null) {
            closeHandler.run();
        } else {
            Renderer.shutdown();
        }
    }

    public @NonNull GUIRoot newFade() {
        return newFade(null, null);
    }

    public @NonNull GUIRoot newFade(@Nullable Fadable fadable, @Nullable UIRenderer renderer) {
        GUIRoot gui_root = createRoot();
        newFade(fadable, gui_root, renderer);
        return gui_root;
    }

    public @NonNull GUIRoot newFade(@Nullable Fadable fadable, @NonNull GUIRoot gui_root,
            @Nullable UIRenderer renderer) {
        fade = new Fade(fadable, gui_root, renderer);
        Renderer.getRenderer().getEventQueue().getManager().registerAnimation(this);
        return gui_root;
    }

    public @NonNull GUIRoot createRoot() {
        GUIRoot gui_root = new GUIRoot(this);
        // This happens early before the viewport is fully initialized
        var window = Renderer.getRenderer().getWindow();
        gui_root.displayChanged(window.getLogicalWidth(), window.getLogicalHeight());
        return gui_root;
    }

    @Override
    public void animate(float t) {
        if (fade != null) {
            fade.animate(this, t);
        }
    }

    void stopFade() {
        Renderer.getRenderer().getEventQueue().getManager().removeAnimation(this);
        fade = null;
    }

    void switchRoot(@NonNull GUIRoot gui_root, @Nullable UIRenderer renderer) {
        current_root.removeTree();
        current_root = gui_root;
        this.renderer = renderer;
    }

    public @NonNull GUIRoot getGUIRoot() {
        return current_root;
    }

    @Nullable
    Fade getFade() {
        return fade;
    }

    public @Nullable UIRenderer getRenderer() {
        return renderer;
    }

    @Override
    public void render() {
        Matrix4f proj = new Matrix4f();
        var guiRoot = getGUIRoot();

        RenderContext context = Renderer.getRenderer().getRenderContext();

        CameraState camera = guiRoot.getDelegate().getCameraState();
        if (camera != null) {
            camera.setView(guiRoot.multProjection(proj.identity()), context.getViewportWidth(),
                    context.getViewportHeight());
            if (!DebugFlags.frustum_freeze) {
                frustum_state.set(camera);
            }
        } else {
            frustum_state.setView(guiRoot.multProjection(proj.identity()), context.getViewportWidth(),
                    context.getViewportHeight());
        }

        if (renderer != null && !renderer.isClosed()) {
            renderer.startFrame(context);
        } else {
            context.clear(true, true);
        }

        if (renderer != null && !renderer.isClosed()) {
            renderer.render(context, frustum_state, current_root);
        }

        if (renderer != null && !renderer.isClosed()) {
            renderer.endFrame(context, this::renderGUI);
        } else {
            renderGUI(context);
        }
    }

    @Override
    public void pickHover() {
        var guiRoot = getGUIRoot();
        CameraState camera = guiRoot.getDelegate().getCameraState();
        GUIObject gui_hit = guiRoot.getCurrentGUIObject();
        if (renderer != null) {
            renderer.pickHover(gui_hit.canHoverBehind(), camera != null ? camera : frustum_state,
                    localInput.getMouseX(), localInput.getMouseY());
        }
    }

    private void renderGUI(@NonNull RenderContext context) {
        GUIRoot guiRoot = getGUIRoot();

        // If we are rendering directly to the back buffer (e.g. loading screen),
        // we must set the correct blend mode here.
        // During gameplay, PostProcessor.renderComposite sets per-buffer blend modes.
        try (var _ = (renderer == null || renderer.isClosed()) ? context.withBlendMode(BlendMode.PREMULTIPLIED)
                : (ScopedState) () -> {
                }) {
            guiRenderer.renderFrame(context, guiRoot.getWidth(), guiRoot.getHeight(), () -> {
                guiRoot.render(guiRenderer);
                guiRoot.renderTopmost(guiRenderer, renderer != null ? renderer.getToolTip() : null, renderer != null
                        && renderer.isCheater());
            });
        }
    }
}
