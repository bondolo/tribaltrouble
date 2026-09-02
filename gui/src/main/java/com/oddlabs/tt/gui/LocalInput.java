package com.oddlabs.tt.gui;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.input.InputManager;
import com.oddlabs.tt.input.InputProvider;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.LWJGL3InputProvider;
import com.oddlabs.tt.input.Modifier;
import com.oddlabs.tt.window.LWJGL3Window;
import com.oddlabs.tt.window.Window;

import com.oddlabs.tt.engine.render.FramePacer;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
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

    private final Window window;
    private final InputProvider<?> inputProvider;
    private final InputManager inputManager;
    private final Deterministic deterministic;
    private final BooleanSupplier developerModeSupplier;
    private final Runnable shutdownAction;
    private final @Nullable FramePacer framePacer;
    private final KeyboardInput keyboardInput;
    private final PointerInput pointerInput;

    private final Set<Key> keys = EnumSet.noneOf(Key.class);
    private final Set<Modifier> global_modifiers = EnumSet.noneOf(Modifier.class);

    public LocalInput(Window lwjglWindow, InputManager inputManager,
            Deterministic deterministic, BooleanSupplier developerModeSupplier,
            Runnable shutdownAction, @Nullable FramePacer framePacer) {
        this.window = lwjglWindow;
        this.inputManager = inputManager;
        this.deterministic = deterministic;
        this.developerModeSupplier = developerModeSupplier;
        this.shutdownAction = shutdownAction;
        this.framePacer = framePacer;
        this.keyboardInput = new KeyboardInput(framePacer);
        if (lwjglWindow instanceof LWJGL3Window win) {
            LWJGL3InputProvider p = new LWJGL3InputProvider(win);
            this.inputProvider = p;
            this.pointerInput = new PointerInput(p, this);
            p.setFocusGainedCallback(this.pointerInput::reapplyCursor);
        } else {
            throw new IllegalStateException("Window is not LWJGL3Window");
        }
    }

    public LocalInput(Window lwjglWindow, InputManager inputManager,
            Deterministic deterministic, BooleanSupplier developerModeSupplier,
            Runnable shutdownAction) {
        this(lwjglWindow, inputManager, deterministic, developerModeSupplier, shutdownAction, null);
    }

    public LocalInput(Window lwjglWindow, InputManager inputManager,
            Deterministic deterministic) {
        this(lwjglWindow, inputManager, deterministic, () -> false, () -> {
        }, null);
    }

    public boolean inDeveloperMode() {
        return developerModeSupplier.getAsBoolean();
    }

    public void shutdown() {
        shutdownAction.run();
    }

    public Window getWindow() {
        return window;
    }

    public Deterministic getDeterministic() {
        return deterministic;
    }

    public void poll(GUIRoot root) {
        pointerInput.poll(root);
        keyboardInput.poll(inputProvider, this, root);
    }

    public void checkMagicKeys() {
        keyboardInput.checkMagicKeys(inputProvider, deterministic);
    }

    public void setKeys(Key key, boolean state, Set<Modifier> modifiers) {
        if (state)
            keys.add(key);
        else
            keys.remove(key);
        global_modifiers.clear();
        global_modifiers.addAll(modifiers);
    }

    public void mouseDragged(GUIRoot gui_root, MouseButton button, short x, short y) {
        mouse_x = x;
        mouse_y = y;
        gui_root.getInputState().mouseDragged(button, x, y);
    }

    public void mouseReleased(GUIRoot gui_root, MouseButton button) {
        gui_root.getInputState().mouseReleased(button);
    }

    public void mousePressed(GUIRoot gui_root, MouseButton button) {
        gui_root.getInputState().mousePressed(button);
    }

    public void mouseScrolled(GUIRoot gui_root, int dz) {
        gui_root.getInputState().mouseScrolled(dz);
    }

    public void mouseScrolledHorizontally(GUIRoot gui_root, int dx) {
        gui_root.getInputState().mouseScrolledHorizontally(dx);
    }

    public void mouseMoved(GUIRoot gui_root, short x, short y) {
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

    public boolean isKeyDown(Key key) {
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
        mouse_x = deterministic.log(inputProvider.getMouseX());
        mouse_y = deterministic.log(inputProvider.getMouseY());
    }

    @Override
    public void close() {
        inputProvider.close();
    }

    public <T> InputProvider<T> getInputProvider() {
        //noinspection unchecked
        return (InputProvider<T>) inputProvider;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public KeyboardInput getKeyboardInput() {
        return keyboardInput;
    }

    public PointerInput getPointerInput() {
        return pointerInput;
    }
}
