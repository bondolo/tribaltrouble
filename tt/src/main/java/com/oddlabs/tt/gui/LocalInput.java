package com.oddlabs.tt.gui;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.input.InputManager;
import com.oddlabs.tt.input.InputProvider;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.LWJGL3InputProvider;
import com.oddlabs.tt.input.Modifier;
import com.oddlabs.tt.window.LWJGL3Window;
import com.oddlabs.tt.window.Window;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Coordinates keyboard and pointer input for the local user.
 */
public final class LocalInput implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(LocalInput.class.getName());

    public static final int CURSOR_ONE_BIT_TRANSPARENCY = 1;
    public static final int CURSOR_8_BIT_ALPHA = 2;

    private int mouse_x;
    private int mouse_y;

    private final @NonNull Window window;
    private final @NonNull InputProvider<?> inputProvider;
    private final @NonNull InputManager inputManager;
    private final KeyboardInput keyboardInput = new KeyboardInput();
    private final @NonNull PointerInput pointerInput;

    private final Set<@NonNull Key> keys = EnumSet.noneOf(Key.class);
    private final Set<@NonNull Modifier> global_modifiers = EnumSet.noneOf(Modifier.class);

    public LocalInput(@NonNull Window lwjglWindow) {
        this.window = lwjglWindow;
        this.inputManager = InputManager.current();
        if (lwjglWindow instanceof LWJGL3Window win) {
            LWJGL3InputProvider p = new LWJGL3InputProvider(win);
            this.inputProvider = p;
            this.pointerInput = new PointerInput(p, this);
            p.setFocusGainedCallback(this.pointerInput::reapplyCursor);
        } else {
            throw new IllegalStateException("Window is not LWJGL3Window");
        }
    }

    public void poll(@NonNull GUIRoot root) {
        pointerInput.poll(root);
        keyboardInput.poll(inputProvider, this, root);
    }

    public void checkMagicKeys() {
        keyboardInput.checkMagicKeys(inputProvider);
    }

    public void setKeys(@NonNull Key key, boolean state, @NonNull Set<@NonNull Modifier> modifiers) {
        if (state)
            keys.add(key);
        else
            keys.remove(key);
        global_modifiers.clear();
        global_modifiers.addAll(modifiers);
    }

    public void mouseDragged(@NonNull GUIRoot gui_root, @NonNull MouseButton button, short x, short y) {
        mouse_x = x;
        mouse_y = y;
        gui_root.getInputState().mouseDragged(button, x, y);
    }

    public void mouseReleased(@NonNull GUIRoot gui_root, @NonNull MouseButton button) {
        gui_root.getInputState().mouseReleased(button);
    }

    public void mousePressed(@NonNull GUIRoot gui_root, @NonNull MouseButton button) {
        gui_root.getInputState().mousePressed(button);
    }

    public void mouseScrolled(@NonNull GUIRoot gui_root, int dz) {
        gui_root.getInputState().mouseScrolled(dz);
    }

    public void mouseScrolledHorizontally(@NonNull GUIRoot gui_root, int dx) {
        gui_root.getInputState().mouseScrolledHorizontally(dx);
    }

    public void mouseMoved(@NonNull GUIRoot gui_root, short x, short y) {
        mouse_x = x;
        mouse_y = y;
        gui_root.getInputState().mouseMoved(x, y);
    }

    public boolean isShiftDownCurrently() {
        return global_modifiers.contains(Modifier.SHIFT);
    }

    public boolean isControlDownCurrently() {
        return global_modifiers.contains(Modifier.CONTROL);
    }

    public boolean isAltDownCurrently() {
        return global_modifiers.contains(Modifier.ALT);
    }

    public boolean isSuperDownCurrently() {
        return global_modifiers.contains(Modifier.META);
    }

    public void resetKeys() {
        keyboardInput.reset(inputProvider);
        inputManager.reset();
        keys.clear();
    }

    public boolean isKeyDown(@NonNull Key key) {
        return keys.contains(key);
    }

    public void resetKeyboard() {
        resetKeys();
        global_modifiers.clear();
    }

    public int getMouseY() {
        return mouse_y;
    }

    public int getMouseX() {
        return mouse_x;
    }

    public void init() {
        if (inputProvider instanceof LWJGL3InputProvider lwjgl3InputProvider) {
            lwjgl3InputProvider.initCallbacks();
        }
        pointerInput.loadCursors(window.getPixelDensity());
        Deterministic deterministic = Renderer.getRenderer().getEventQueue().getDeterministic();
        mouse_x = deterministic.log(inputProvider.getMouseX());
        mouse_y = deterministic.log(inputProvider.getMouseY());
    }

    @Override
    public void close() {
        inputProvider.close();
    }

    public <T> @NonNull InputProvider<T> getInputProvider() {
        //noinspection unchecked
        return (InputProvider<T>) inputProvider;
    }

    public @NonNull InputManager getInputManager() {
        return inputManager;
    }

    public @NonNull KeyboardInput getKeyboardInput() {
        return keyboardInput;
    }

    public @NonNull PointerInput getPointerInput() {
        return pointerInput;
    }
}
