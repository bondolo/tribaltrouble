package com.oddlabs.tt.input;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.gui.Cursor;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.gui.CursorFile;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Manages mouse/pointer input and hardware cursor state.
 */
public final class PointerInput {
    private static final Logger logger = Logger.getLogger(PointerInput.class.getName());
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    private final Set<@NonNull MouseButton> buttons = EnumSet.noneOf(MouseButton.class);
    private short last_x;
    private short last_y;
    private @NonNull Cursor active_cursor = Cursor.NULL_CURSOR;
    private @Nullable CursorType active_cursor_type = null;
    private @Nullable MouseButton drag_button = null;

    private final @NonNull InputProvider<?> inputProvider;
    private final @NonNull LocalInput localInput;

    private final Map<@NonNull CursorType, @NonNull Cursor> cursors = new EnumMap<>(CursorType.class);
    private @NonNull Cursor debug_cursor = Cursor.NULL_CURSOR;

    private float current_scale = 1.0f;
    private float current_pixel_density = 1.0f;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public float getCurrentScale() {
        return current_scale;
    }

    public float getCurrentPixelDensity() {
        return current_pixel_density;
    }

    public void loadCursors(float scale) {
        float density = Renderer.getRenderer().getWindow().getPixelDensity();
        loadCursors(scale, density);
    }

    public void loadCursors(float scale, float pixelDensity) {
        int w = Renderer.getRenderer().getWindow().getWidth();
        int h = Renderer.getRenderer().getWindow().getHeight();
        if (current_scale == scale && current_pixel_density == pixelDensity && lastWidth == w && lastHeight == h) {
            return;
        }
        lastWidth = w;
        lastHeight = h;
        this.current_scale = scale;
        this.current_pixel_density = pixelDensity;
        float imageScale = scale;
        if (IS_MAC) {
            // MacOS will scale the cursor in retina mode so reuse the scale.
            imageScale /= pixelDensity;
        }

        // Close old cursors to prevent native memory leaks and stale cursor handles
        if (debug_cursor != Cursor.NULL_CURSOR) {
            debug_cursor.close();
        }
        for (Cursor c : cursors.values()) {
            if (c != Cursor.NULL_CURSOR && c != debug_cursor) {
                c.close();
            }
        }
        cursors.clear();

        debug_cursor = new CursorFile("/textures/gui/pointer_clientload_32_8.png", 2, 2, imageScale).get();
        cursors.put(CursorType.NORMAL, new CursorFile("/textures/gui/pointer_32_8.png", 2, 2, imageScale).get());
        cursors.put(CursorType.TARGET, new CursorFile("/textures/gui/pointer_target_32_8.png", 14, 14, imageScale)
                .get());
        cursors.put(CursorType.TEXT, new CursorFile("/textures/gui/pointer_text_32_8.png", 6, 11, imageScale).get());
        cursors.put(CursorType.DEBUG, debug_cursor);
        cursors.put(CursorType.NULL, Cursor.NULL_CURSOR);

        if (active_cursor_type != null) {
            setActiveCursor(active_cursor_type);
        }
    }

    public PointerInput(@NonNull InputProvider<?> inputProvider, @NonNull LocalInput localInput) {
        this.inputProvider = inputProvider;
        this.localInput = localInput;
    }

    public void reapplyCursor() {
        if (active_cursor_type != null) {
            Cursor c = cursors.get(active_cursor_type);
            if (c != null) {
                doSetActiveCursor(c);
            }
        }
    }

    public void setActiveCursor(@NonNull CursorType type) {
        this.active_cursor_type = type;
        Cursor c = cursors.get(type);
        if (c != null) {
            setActiveCursor(c);
        }
    }

    public void setActiveCursor(@NonNull Cursor cursor) {
        if (cursor != Cursor.NULL_CURSOR && inputProvider.isGrabbed()) {
            inputProvider.setGrabbed(false);
            resetCursorPos();
        } else if (cursor == Cursor.NULL_CURSOR && !inputProvider.isGrabbed()) {
            inputProvider.setGrabbed(true);
            resetCursorPos();
        }
        if (active_cursor != cursor) {
            doSetActiveCursor(cursor);
        }
    }

    public void setCursorPosition(int x, int y) {
        if (!Renderer.getRenderer().getEventQueue().getDeterministic().isPlayback())
            inputProvider.setCursorPosition(x, y);
    }

    private void resetCursorPos() {
        setCursorPosition(localInput.getMouseX(), localInput.getMouseY());
        // clear event queue
        while (inputProvider.nextMouseEvent())
            ;
    }

    private void doSetActiveCursor(@NonNull Cursor cursor) {
        active_cursor = cursor;
        //noinspection unchecked
        InputProvider<Long> provider = (InputProvider<Long>) inputProvider;

        var useCursor = Renderer.getRenderer().getEventQueue().getDeterministic().isPlayback()
                ? debug_cursor : cursor;
        provider.setNativeCursor(useCursor.getCursor());
    }

    public void deletingCursor(@NonNull Cursor cursor) {
        if (active_cursor == cursor) {
            doSetActiveCursor(Cursor.NULL_CURSOR);
        }
    }

    private void updateMouse(@NonNull GUIRoot gui_root, int x, int y, int dz, int dx) {
        if (x != last_x || y != last_y) {
            last_x = (short) x;
            last_y = (short) y;
            if (drag_button != null && buttons.contains(drag_button)) {
                localInput.mouseDragged(gui_root, drag_button, last_x, last_y);
            } else {
                localInput.mouseMoved(gui_root, last_x, last_y);
            }
        }
        if (dz != 0)
            localInput.mouseScrolled(gui_root, dz);
        if (dx != 0)
            localInput.mouseScrolledHorizontally(gui_root, dx);
    }

    public void poll(@NonNull GUIRoot gui_root) {
        Deterministic deterministic = Renderer.getRenderer().getEventQueue().getDeterministic();
        inputProvider.pollMouse();
        int accum_x = last_x;
        int accum_y = last_y;
        int accum_dz = 0;
        int accum_dx = 0;
        while (deterministic.log(inputProvider.nextMouseEvent())) {
            accum_x = deterministic.log(inputProvider.getEventX());
            accum_y = deterministic.log(inputProvider.getEventY());
            accum_dz += deterministic.log(inputProvider.getEventDWheel());
            accum_dx += deterministic.log(inputProvider.getEventDWheelX());
            MouseButton button = MouseButton.fromInt(deterministic.log(inputProvider.getEventButton()));
            if (button != null) {
                updateMouse(gui_root, accum_x, accum_y, accum_dz, accum_dx);
                accum_dz = 0;
                accum_dx = 0;
                if (deterministic.log(inputProvider.getEventButtonState())) {
                    if (buttons.add(button)) {
                        if (drag_button == null) {
                            drag_button = button;
                        }
                        localInput.mousePressed(gui_root, button);
                    }
                } else {
                    if (buttons.remove(button)) {
                        drag_button = null;
                        localInput.mouseReleased(gui_root, button);
                    }
                }
            }
        }
        updateMouse(gui_root, accum_x, accum_y, accum_dz, accum_dx);
    }
}
