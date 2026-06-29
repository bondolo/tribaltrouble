package com.oddlabs.tt.input;

import com.oddlabs.tt.window.LWJGL3Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.sdl.SDL_MouseButtonEvent;
import org.lwjgl.sdl.SDL_MouseMotionEvent;
import org.lwjgl.sdl.SDL_MouseWheelEvent;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_MOTION;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_WHEEL;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_INPUT;
import static org.lwjgl.sdl.SDLKeyboard.SDL_GetKeyboardState;
import static org.lwjgl.sdl.SDLKeyboard.SDL_StartTextInput;
import static org.lwjgl.sdl.SDLKeyboard.SDL_StopTextInput;
import static org.lwjgl.sdl.SDLMouse.SDL_GetDefaultCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_GetMouseState;
import static org.lwjgl.sdl.SDLMouse.SDL_GetWindowRelativeMouseMode;
import static org.lwjgl.sdl.SDLMouse.SDL_SetCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_SetWindowRelativeMouseMode;
import static org.lwjgl.sdl.SDLMouse.SDL_WarpMouseInWindow;

/**
 * SDL 3 based implementation of the InputProvider interface.
 */
public final class LWJGL3InputProvider implements InputProvider<Long> {

    private static final Logger logger = Logger.getLogger(LWJGL3InputProvider.class.getName());

    private final @NonNull LWJGL3Window window;
    private long windowHandle;

    // Keyboard State
    // @GuardedBy("this")
    private final Deque<@NonNull KeyEvent> keyEvents = new ArrayDeque<>();
    private @Nullable KeyEvent currentKeyEvent;

    // Mouse State
    private final Deque<@NonNull MouseEvent> mouseEvents = new ArrayDeque<>();
    private @Nullable MouseEvent currentMouseEvent;
    private double mouseX, mouseY;
    private long currentNativeCursor = MemoryUtil.NULL;

    private static class KeyEvent {
        final int key;
        final boolean pressed;
        final int scancode;
        final int mods;
        final boolean repeat;
        int codepoint;

        KeyEvent(int key, boolean pressed, int scancode, int mods, boolean repeat) {
            this.key = key;
            this.pressed = pressed;
            this.scancode = scancode;
            this.mods = mods;
            this.repeat = repeat;
        }

        KeyEvent(int key, boolean pressed, int scancode, int mods, boolean repeat, int codepoint) {
            this(key, pressed, scancode, mods, repeat);
            this.codepoint = codepoint;
        }
    }

    private record MouseEvent(int button, boolean state, int x, int y, int dWheel, int dWheelX) {
    }

    public LWJGL3InputProvider(@NonNull LWJGL3Window win) {
        this.window = win;
    }

    public void initCallbacks() {
        this.windowHandle = window.getHandle();
        if (windowHandle == MemoryUtil.NULL) {
            throw new IllegalStateException("Window handle is NULL. Window might not be created yet.");
        }
        window.setInputProvider(this);
        SDL_StartTextInput(windowHandle);
    }

    public void processEvent(@NonNull SDL_Event event) {
        switch (event.type()) {
            case SDL_EVENT_KEY_DOWN, SDL_EVENT_KEY_UP -> {
                SDL_KeyboardEvent keyEvent = event.key();
                int scancode = keyEvent.scancode();
                boolean pressed = event.type() == SDL_EVENT_KEY_DOWN;
                int mods = keyEvent.mod();
                boolean repeat = keyEvent.repeat();

                var e = new KeyEvent(scancode, pressed, scancode, mods, repeat);
                synchronized (keyEvents) {
                    keyEvents.add(e);
                }
            }
            case SDL_EVENT_TEXT_INPUT -> {
                // TODO: Handle multi-codepoint strings from SDL text input (e.g., for IME/emoji).
                // Currently only the first codepoint is taken.
                String text = event.text().textString();
                if (!text.isEmpty()) {
                    int c = text.codePointAt(0);
                    synchronized (keyEvents) {
                        if (!keyEvents.isEmpty()) {
                            KeyEvent last = keyEvents.getLast();
                            if (last.pressed) {
                                last.codepoint = c;
                                return;
                            }
                        }
                        keyEvents.add(new KeyEvent(0, true, 0, 0, false, c));
                    }
                }
            }
            case SDL_EVENT_MOUSE_MOTION -> {
                if (currentNativeCursor != MemoryUtil.NULL) {
                    SDL_SetCursor(currentNativeCursor);
                }
                SDL_MouseMotionEvent motionEvent = event.motion();
                int logicalHeight = window.getLogicalHeight();
                this.mouseX = motionEvent.x();
                this.mouseY = logicalHeight - motionEvent.y() - 1;
                synchronized (mouseEvents) {
                    mouseEvents.add(new MouseEvent(-1, false, (int) mouseX, (int) mouseY, 0, 0));
                }
            }
            case SDL_EVENT_MOUSE_BUTTON_DOWN, SDL_EVENT_MOUSE_BUTTON_UP -> {
                SDL_MouseButtonEvent buttonEvent = event.button();
                boolean pressed = event.type() == SDL_EVENT_MOUSE_BUTTON_DOWN;
                int sdlButton = buttonEvent.button();
                int button = switch (sdlButton) {
                    case 1 -> 0; // SDL_BUTTON_LEFT -> Left
                    case 3 -> 1; // SDL_BUTTON_RIGHT -> Right
                    case 2 -> 2; // SDL_BUTTON_MIDDLE -> Middle
                    default -> sdlButton - 1;
                };
                synchronized (mouseEvents) {
                    mouseEvents.add(new MouseEvent(button, pressed, (int) mouseX, (int) mouseY, 0, 0));
                }
            }
            case SDL_EVENT_MOUSE_WHEEL -> {
                SDL_MouseWheelEvent wheelEvent = event.wheel();
                synchronized (mouseEvents) {
                    mouseEvents.add(new MouseEvent(-1, false, (int) mouseX, (int) mouseY, (int) (wheelEvent.y()
                            * 120), (int) (wheelEvent.x() * 120)));
                }
            }
        }
    }

    @Override
    public void close() {
        if (windowHandle != MemoryUtil.NULL) {
            SDL_StopTextInput(windowHandle);
            windowHandle = MemoryUtil.NULL;
        }
        window.setInputProvider(null);
    }

    @Override
    public void pollKeyboard() {
        // Events populated by callbacks
    }

    @Override
    public boolean nextKeyboardEvent() {
        synchronized (keyEvents) {
            if (keyEvents.isEmpty()) return false;
            currentKeyEvent = keyEvents.poll();
            return true;
        }
    }

    @Override
    public int getEventKey() {
        return currentKeyEvent != null ? currentKeyEvent.key : 0;
    }

    @Override
    public boolean getEventKeyState() {
        return currentKeyEvent != null && currentKeyEvent.pressed;
    }

    @Override
    public int getEventKeyMods() {
        return currentKeyEvent != null ? currentKeyEvent.mods : 0;
    }

    @Override
    public int getEventCodepoint() {
        return currentKeyEvent != null ? currentKeyEvent.codepoint : 0;
    }

    @Override
    public boolean isRepeatEvent() {
        return currentKeyEvent != null && currentKeyEvent.repeat;
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        ByteBuffer state = SDL_GetKeyboardState();
        return state != null && state.get(keyCode) != 0;
    }

    @Override
    public void pollMouse() {
        // Populated by processEvent
    }

    @Override
    public boolean nextMouseEvent() {
        synchronized (mouseEvents) {
            if (mouseEvents.isEmpty()) return false;
            currentMouseEvent = mouseEvents.poll();
            return true;
        }
    }

    @Override
    public int getEventButton() {
        return currentMouseEvent != null ? currentMouseEvent.button() : -1;
    }

    @Override
    public boolean getEventButtonState() {
        return currentMouseEvent != null && currentMouseEvent.state();
    }

    @Override
    public int getEventDWheel() {
        return currentMouseEvent != null ? currentMouseEvent.dWheel() : 0;
    }

    @Override
    public int getEventDWheelX() {
        return currentMouseEvent != null ? currentMouseEvent.dWheelX() : 0;
    }

    @Override
    public int getEventX() {
        return currentMouseEvent != null ? currentMouseEvent.x() : (int) mouseX;
    }

    @Override
    public int getEventY() {
        return currentMouseEvent != null ? currentMouseEvent.y() : (int) mouseY;
    }

    @Override
    public int getMouseX() {
        return (int) mouseX;
    }

    @Override
    public int getMouseY() {
        return (int) mouseY;
    }

    @Override
    public boolean isButtonDown(int button) {
        int sdlButton = switch (button) {
            case 0 -> 1; // Left
            case 1 -> 3; // Right
            case 2 -> 2; // Middle
            default -> button + 1;
        };
        int mask = SDL_GetMouseState(null, null);
        return (mask & (1 << (sdlButton - 1))) != 0;
    }

    @Override
    public void setCursorPosition(int x, int y) {
        if (windowHandle == MemoryUtil.NULL) return;
        // Coordinates passed are in logical game units (the same as mouseMoved)
        float screenX = (float) x;
        float screenY = (float) (window.getLogicalHeight() - y - 1);
        SDL_WarpMouseInWindow(windowHandle, screenX, screenY);
    }

    @Override
    public void setGrabbed(boolean grabbed) {
        SDL_SetWindowRelativeMouseMode(windowHandle, grabbed);
    }

    @Override
    public boolean isGrabbed() {
        return SDL_GetWindowRelativeMouseMode(windowHandle);
    }

    @Override
    public void setNativeCursor(@Nullable Long cursor) {
        if (windowHandle == MemoryUtil.NULL) return;

        if (null != cursor && cursor != MemoryUtil.NULL) {
            currentNativeCursor = cursor;
            SDL_SetCursor(cursor);
        } else {
            currentNativeCursor = MemoryUtil.NULL;
            long defaultCursor = SDL_GetDefaultCursor();
            if (defaultCursor != MemoryUtil.NULL) {
                SDL_SetCursor(defaultCursor);
            }
        }
    }
}
