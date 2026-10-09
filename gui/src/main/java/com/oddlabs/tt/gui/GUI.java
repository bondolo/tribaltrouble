package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.event.LocalEventQueue;
import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.state.BlendMode;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.render.SceneHoverProvider;
import com.oddlabs.tt.gui.render.WorldSceneRenderer;
import com.oddlabs.tt.window.Window;
import com.oddlabs.tt.window.WindowSettings;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Container for the 2D user interface
 */
public final class GUI implements Animated, AutoCloseable {
    private final Skin skin;
    private final LocalInput localInput;
    private final Window window;
    private final LocalEventQueue eventQueue;
    private final Settings settings;
    private final Runnable shutdownHandler;
    private final @Nullable Consumer<GUI> progressUpdater;
    private final DoubleSupplier fpsSupplier;
    private @Nullable Runnable movieRecordingStarter;
    private final GUIRenderer guiRenderer = new GUIRenderer();
    private GUIRoot current_root;
    private @Nullable Fade fade;
    private @Nullable WorldSceneRenderer renderer;
    private @Nullable SceneHoverProvider hoverProvider;
    private @Nullable Runnable closeHandler;
    private @Nullable Runnable errorAudioHandler;

    public GUI(LocalInput localInput, Skin skin, Window window, LocalEventQueue eventQueue, Settings settings,
            Runnable shutdownHandler, @Nullable Consumer<GUI> progressUpdater, DoubleSupplier fpsSupplier) {
        this.localInput = localInput;
        this.skin = skin;
        this.window = window;
        this.eventQueue = eventQueue;
        this.settings = settings;
        this.shutdownHandler = shutdownHandler;
        this.progressUpdater = progressUpdater;
        this.fpsSupplier = fpsSupplier;
        this.current_root = createRoot();
    }

    public GUI(LocalInput localInput, Window window, LocalEventQueue eventQueue, Settings settings,
            Runnable shutdownHandler, @Nullable Consumer<GUI> progressUpdater, DoubleSupplier fpsSupplier) {
        this(localInput, new Skin("/gui/gui_skin.xml"), window, eventQueue, settings, shutdownHandler, progressUpdater,
                fpsSupplier);
    }

    public GUI(LocalInput localInput, Window window, LocalEventQueue eventQueue, Settings settings,
            Runnable shutdownHandler, @Nullable Consumer<GUI> progressUpdater) {
        this(localInput, window, eventQueue, settings, shutdownHandler, progressUpdater, () -> 0.0);
    }

    public DoubleSupplier getFpsSupplier() {
        return fpsSupplier;
    }

    public Window getWindow() {
        return window;
    }

    public LocalEventQueue getEventQueue() {
        return eventQueue;
    }

    public AnimationManager getAnimationManager() {
        return eventQueue.getManager();
    }

    public Settings getSettings() {
        return settings;
    }

    public Runnable getShutdownHandler() {
        return shutdownHandler;
    }

    public float getTime() {
        return eventQueue.getTime();
    }

    public void setMovieRecordingStarter(@Nullable Runnable movieRecordingStarter) {
        this.movieRecordingStarter = movieRecordingStarter;
    }

    public void setErrorAudioHandler(@Nullable Runnable handler) {
        this.errorAudioHandler = handler;
    }

    public void playErrorAudio() {
        var handler = errorAudioHandler;
        if (handler != null) {
            try {
                handler.run();
            } catch (Exception _) {
                // Ignore audio errors
            }
        }
    }

    public void startMovieRecording() {
        if (movieRecordingStarter != null) {
            movieRecordingStarter.run();
        }
    }

    public void toggleFullscreen() {
        try {
            boolean fs = !window.isFullscreen() && !eventQueue.getDeterministic().isPlayback();
            window.setFullscreen(fs);
            WindowSettings.from(settings).fullscreen = fs;
        } catch (Exception e) {
            throw new IllegalStateException("Mode switching failed", e);
        }
    }

    public Skin getSkin() {
        return skin;
    }

    public LocalInput getLocalInput() {
        return localInput;
    }

    public void setCloseHandler(@Nullable Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }

    public void tick() {
        localInput.poll(getGUIRoot());
    }

    public void onCloseRequested() {
        if (closeHandler != null) {
            closeHandler.run();
        } else {
            shutdownHandler.run();
        }
    }

    public void updateProgress() {
        if (progressUpdater != null) {
            progressUpdater.accept(this);
        }
    }

    public GUIRoot newFade() {
        return newFade(null, (WorldSceneRenderer) null);
    }

    public GUIRoot newFade(@Nullable Runnable onComplete, @Nullable WorldSceneRenderer renderer) {
        return newFade(onComplete, renderer, renderer instanceof SceneHoverProvider hover ? hover : null);
    }

    public GUIRoot newFade(@Nullable Runnable onComplete, @Nullable WorldSceneRenderer renderer,
            @Nullable SceneHoverProvider hoverProvider) {
        GUIRoot gui_root = createRoot();
        newFade(onComplete, gui_root, renderer, hoverProvider);
        return gui_root;
    }

    public GUIRoot newFade(@Nullable Runnable onComplete, GUIRoot gui_root,
            @Nullable WorldSceneRenderer renderer) {
        return newFade(onComplete, gui_root, renderer, renderer instanceof SceneHoverProvider hover ? hover : null);
    }

    public GUIRoot newFade(@Nullable Runnable onComplete, GUIRoot gui_root,
            @Nullable WorldSceneRenderer renderer, @Nullable SceneHoverProvider hoverProvider) {
        fade = new Fade(onComplete, gui_root, renderer, hoverProvider);
        eventQueue.getManager().registerAnimation(this);
        return gui_root;
    }

    public GUIRoot createRoot() {
        GUIRoot gui_root = new GUIRoot(this, skin);
        // This happens early before the viewport is fully initialized
        gui_root.displayChanged(window.getWidth(), window.getHeight());
        return gui_root;
    }

    @Override
    public void animate(float dt) {
        if (fade != null) {
            fade.animate(this, dt);
        }
    }

    void stopFade() {
        eventQueue.getManager().removeAnimation(this);
        fade = null;
    }

    void switchRoot(GUIRoot gui_root, @Nullable WorldSceneRenderer renderer) {
        switchRoot(gui_root, renderer, renderer instanceof SceneHoverProvider hover ? hover : null);
    }

    void switchRoot(GUIRoot gui_root, @Nullable WorldSceneRenderer renderer,
            @Nullable SceneHoverProvider hoverProvider) {
        current_root.removeTree();
        current_root = gui_root;
        if (this.renderer != null && this.renderer != renderer) {
            this.renderer.close();
        }
        this.renderer = renderer;
        this.hoverProvider = hoverProvider;
    }

    @Override
    public void close() {
        guiRenderer.close();
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
        hoverProvider = null;
    }

    public GUIRoot getGUIRoot() {
        return current_root;
    }

    @Nullable
    Fade getFade() {
        return fade;
    }

    public @Nullable WorldSceneRenderer getRenderer() {
        return renderer;
    }

    public @Nullable SceneHoverProvider getHoverProvider() {
        return hoverProvider;
    }

    public void pickHover(CameraState cameraState) {
        var guiRoot = getGUIRoot();
        GUIObject gui_hit = guiRoot.getCurrentGUIObject();
        if (hoverProvider != null) {
            hoverProvider.pickHover(gui_hit.canHoverBehind(), cameraState,
                    localInput.getMouseX(), localInput.getMouseY());
        }
    }

    public void render(RenderContext context) {
        GUIRoot guiRoot = getGUIRoot();

        try (var _ = context.withBlendMode(BlendMode.PREMULTIPLIED)) {
            guiRenderer.renderFrame(context, guiRoot.getWidth(), guiRoot.getHeight(), () -> {
                guiRoot.render(guiRenderer);
                guiRoot.renderTopmost(guiRenderer, hoverProvider != null ? hoverProvider.getToolTip() : null);
            });
        }
    }
}
