package com.oddlabs.tt.gui;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.event.LocalEventQueue;
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
import org.jspecify.annotations.Nullable;

/**
 * Container for the 2D user interface
 */
public final class GUI implements Animated, FrameDriver {
    private final Skin skin;
    private final LocalInput localInput;
    private final LocalEventQueue eventQueue;
    private final GUIRenderer guiRenderer = new GUIRenderer();
    private GUIRoot current_root;
    private @Nullable Fade fade;
    private @Nullable UIRenderer renderer;
    private final CameraState frustum_state = new CameraState();
    private @Nullable Runnable closeHandler;

    public GUI(LocalInput localInput, LocalEventQueue eventQueue, Skin skin) {
        this.localInput = localInput;
        this.eventQueue = eventQueue;
        this.skin = skin;
        this.current_root = createRoot();
    }

    public GUI(LocalInput localInput, LocalEventQueue eventQueue) {
        this(localInput, eventQueue, new Skin("/gui/gui_skin.xml"));
    }

    public GUI(LocalInput localInput) {
        this(localInput, Renderer.getRenderer().getEventQueue());
    }

    public LocalEventQueue getEventQueue() {
        return eventQueue;
    }

    public AnimationManager getAnimationManager() {
        return eventQueue.getManager();
    }

    public float getTime() {
        return eventQueue.getTime();
    }

    public Skin getSkin() {
        return skin;
    }

    public void runWithSkin(Runnable operation) {
        Skin.run(skin, operation);
    }

    public <V, X extends Throwable> V callWithSkin(ScopedValue.CallableOp<V, X> operation) throws X {
        return Skin.call(skin, operation);
    }

    public LocalInput getLocalInput() {
        return localInput;
    }

    public void setCloseHandler(@Nullable Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }

    @Override
    public void run(Runnable session) {
        runWithSkin(session);
    }

    @Override
    public void tick(NetworkSelector network) {
        ScopedValue.where(Skin.CURRENT, skin).run(() -> localInput.poll(getGUIRoot()));
    }

    @Override
    public void onCloseRequested() {
        ScopedValue.where(Skin.CURRENT, skin).run(() -> {
            if (closeHandler != null) {
                closeHandler.run();
            } else {
                Renderer.shutdown();
            }
        });
    }

    public GUIRoot newFade() {
        return newFade(null, null);
    }

    public GUIRoot newFade(@Nullable Fadable fadable, @Nullable UIRenderer renderer) {
        GUIRoot gui_root = createRoot();
        newFade(fadable, gui_root, renderer);
        return gui_root;
    }

    public GUIRoot newFade(@Nullable Fadable fadable, GUIRoot gui_root,
            @Nullable UIRenderer renderer) {
        fade = new Fade(fadable, gui_root, renderer);
        eventQueue.getManager().registerAnimation(this);
        return gui_root;
    }

    public GUIRoot createRoot() {
        return ScopedValue.where(Skin.CURRENT, skin).call(() -> {
            GUIRoot gui_root = new GUIRoot(this);
            // This happens early before the viewport is fully initialized
            var window = Renderer.getRenderer().getWindow();
            gui_root.displayChanged(window.getLogicalWidth(), window.getLogicalHeight());
            return gui_root;
        });
    }

    @Override
    public void animate(float t) {
        if (fade != null) {
            ScopedValue.where(Skin.CURRENT, skin).run(() -> fade.animate(this, t));
        }
    }

    void stopFade() {
        eventQueue.getManager().removeAnimation(this);
        fade = null;
    }

    void switchRoot(GUIRoot gui_root, @Nullable UIRenderer renderer) {
        current_root.removeTree();
        current_root = gui_root;
        this.renderer = renderer;
    }

    public GUIRoot getGUIRoot() {
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

    private void renderGUI(RenderContext context) {
        GUIRoot guiRoot = getGUIRoot();

        // If we are rendering directly to the back buffer (e.g. loading screen),
        // we must set the correct blend mode here.
        // During gameplay, PostProcessor.renderComposite sets per-buffer blend modes.
        try (var _ = (renderer == null || renderer.isClosed()) ? context.withBlendMode(BlendMode.PREMULTIPLIED)
                : (ScopedState) () -> {
                }) {
            ScopedValue.where(Skin.CURRENT, skin).run(() -> {
                guiRenderer.renderFrame(context, guiRoot.getWidth(), guiRoot.getHeight(), () -> {
                    guiRoot.render(guiRenderer);
                    guiRoot.renderTopmost(guiRenderer, renderer != null ? renderer.getToolTip() : null, renderer != null
                            && renderer.isCheater());
                });
            });
        }
    }
}
