package com.oddlabs.tt.client.screen;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.Nullable;

/**
 * Manages active application screens, transitions, and coordinates 3D scene rendering with 2D GUI presentation.
 */
public final class ScreenManager implements Animated, AutoCloseable {
    private final GUI gui;
    private final AnimationManager animationManager;
    private final Runnable updateProgress;
    private Screen currentScreen;
    private @Nullable Fade fade;

    public ScreenManager(GUI gui, AnimationManager animationManager, Runnable updateProgress) {
        this.gui = gui;
        this.animationManager = animationManager;
        this.updateProgress = updateProgress;
        this.currentScreen = new DefaultScreen(gui.createRoot());
        gui.setRoot(this.currentScreen.guiRoot());
    }

    public void updateProgress() {
        updateProgress.run();
    }

    public GUI getGUI() {
        return gui;
    }

    public Screen getScreen() {
        return currentScreen;
    }

    public GUIRoot createRoot() {
        return gui.createRoot();
    }

    public void switchScreen(Screen newScreen) {
        Screen oldScreen = this.currentScreen;
        oldScreen.guiRoot().detach();
        if (oldScreen.sceneRenderer() != null && oldScreen.sceneRenderer() != newScreen.sceneRenderer()) {
            oldScreen.sceneRenderer().close();
        }
        this.currentScreen = newScreen;
        gui.setRoot(newScreen.guiRoot());
    }

    public GUIRoot fadeTo(Screen screen) {
        return fadeTo(screen, null);
    }

    public GUIRoot fadeTo(Screen screen, @Nullable Runnable onComplete) {
        fade = new Fade(onComplete, screen);
        animationManager.registerAnimation(this);
        return screen.guiRoot();
    }

    public GUIRoot fadeToDefault() {
        return fadeToDefault(null);
    }

    public GUIRoot fadeToDefault(@Nullable Runnable onComplete) {
        return fadeTo(new DefaultScreen(gui.createRoot()), onComplete);
    }

    public GUIRoot fadeToRoot(GUIRoot guiRoot) {
        return fadeToRoot(guiRoot, null);
    }

    public GUIRoot fadeToRoot(GUIRoot guiRoot, @Nullable Runnable onComplete) {
        return fadeTo(currentScreen.withGUIRoot(guiRoot), onComplete);
    }

    void stopFade() {
        animationManager.removeAnimation(this);
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
            GUIObject guiHit = currentScreen.guiRoot().getCurrentGUIObject();
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
    }
}
