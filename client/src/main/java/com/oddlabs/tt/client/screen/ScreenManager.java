package com.oddlabs.tt.client.screen;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Manages active application screens, transitions, and coordinates 3D scene rendering with 2D GUI presentation.
 */
public final class ScreenManager implements Animated, AutoCloseable {
    private final Peer engine;
    private final GUI gui;
    private Screen currentScreen;
    private @Nullable Fade fade;

    public ScreenManager(Peer engine, GUI gui) {
        this.engine = Objects.requireNonNull(engine, "engine cannot be null");
        this.gui = Objects.requireNonNull(gui, "gui cannot be null");
        this.currentScreen = new DefaultScreen(gui.createRoot());
        gui.setRoot(this.currentScreen.guiRoot());
        engine.setScreenManager(this);
    }

    public Peer getEngine() {
        return engine;
    }

    public GUI getGUI() {
        return gui;
    }

    public Screen getScreen() {
        return currentScreen;
    }

    public GUIRoot getGUIRoot() {
        return currentScreen.guiRoot();
    }

    public GUIRoot createRoot() {
        return gui.createRoot();
    }

    public void switchScreen(Screen newScreen) {
        Screen oldScreen = this.currentScreen;
        if (oldScreen != null) {
            oldScreen.guiRoot().detach();
            if (oldScreen.sceneRenderer() != null && oldScreen.sceneRenderer() != newScreen.sceneRenderer()) {
                oldScreen.sceneRenderer().close();
            }
        }
        this.currentScreen = Objects.requireNonNull(newScreen, "newScreen cannot be null");
        gui.setRoot(newScreen.guiRoot());
    }

    public GUIRoot newFade() {
        return newFade((Runnable) null);
    }

    public GUIRoot newFade(@Nullable Runnable onComplete) {
        return newFade(onComplete, new DefaultScreen(gui.createRoot()));
    }

    public GUIRoot newFade(@Nullable Runnable onComplete, Screen screen) {
        fade = new Fade(onComplete, screen);
        engine.getEventQueue().getManager().registerAnimation(this);
        return screen.guiRoot();
    }

    void stopFade() {
        engine.getEventQueue().getManager().removeAnimation(this);
        fade = null;
    }

    @Override
    public void animate(float dt) {
        if (fade != null) {
            fade.animate(this, dt);
        }
    }

    public void pickHover(CameraState cameraState, int mouseX, int mouseY) {
        var hoverProvider = currentScreen.hoverProvider();
        if (hoverProvider != null) {
            GUIObject guiHit = getGUIRoot().getCurrentGUIObject();
            hoverProvider.pickHover(guiHit.canHoverBehind(), cameraState, mouseX, mouseY);
        }
    }

    public void renderUI(RenderContext context) {
        var hoverProvider = currentScreen.hoverProvider();
        gui.render(context, hoverProvider != null ? hoverProvider.getToolTip() : null,
                fade != null ? renderer -> fade.render(renderer, currentScreen.guiRoot().getWidth(), currentScreen
                        .guiRoot().getHeight()) : null);
    }

    @Override
    public void close() {
        gui.close();
        currentScreen.close();
        engine.setScreenManager(null);
    }
}
