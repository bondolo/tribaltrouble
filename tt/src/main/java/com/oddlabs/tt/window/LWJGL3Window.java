package com.oddlabs.tt.window;

import com.oddlabs.tt.input.LWJGL3InputProvider;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SerializableDisplayMode;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.macosx.ObjCRuntime;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO;
import static org.lwjgl.sdl.SDLInit.SDL_Init;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLPixels.SDL_BITSPERPIXEL;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ABGR8888;
import static org.lwjgl.sdl.SDLProperties.SDL_GetNumberProperty;
import static org.lwjgl.sdl.SDLStdinc.nSDL_free;
import static org.lwjgl.sdl.SDLSurface.SDL_CreateSurfaceFrom;
import static org.lwjgl.sdl.SDLSurface.SDL_DestroySurface;
import static org.lwjgl.sdl.SDLVideo.*;

/**
 * SDL 3 based implementation of the Window interface.
 */
public final class LWJGL3Window implements Window {
    private static final Logger logger = Logger.getLogger(LWJGL3Window.class.getSimpleName());
    private static final String os = System.getProperty("os.name").toLowerCase();
    private static final boolean isMac = os.contains("mac");

    private static final boolean DEBUG = Boolean.getBoolean("com.oddlabs.tt.developer");
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private static long objc_msgSend = MemoryUtil.NULL;
    private static long nsAppClass = MemoryUtil.NULL;
    private static long sharedApplicationSel = MemoryUtil.NULL;
    private static long isActiveSel = MemoryUtil.NULL;
    private static long setPresentationOptionsSel = MemoryUtil.NULL;
    private static long nsApp = MemoryUtil.NULL;
    private static boolean macInitialized = false;

    private static void initMacFFI() {
        if (isMac && !macInitialized) {
            try {
                objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                nsAppClass = ObjCRuntime.objc_getClass("NSApplication");
                sharedApplicationSel = ObjCRuntime.sel_getUid("sharedApplication");
                isActiveSel = ObjCRuntime.sel_getUid("isActive");
                setPresentationOptionsSel = ObjCRuntime.sel_getUid("setPresentationOptions:");
                nsApp = JNI.invokePPP(nsAppClass, sharedApplicationSel, objc_msgSend);
                macInitialized = true;
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to initialize macOS FFI caching", t);
            }
        }
    }

    private boolean isMacAppActive() {
        if (isMac) {
            initMacFFI();
            if (macInitialized && nsApp != MemoryUtil.NULL) {
                try {
                    return JNI.invokePPZ(nsApp, isActiveSel, objc_msgSend);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Failed to call [NSApp isActive]", t);
                }
            }
        }
        return false;
    }

    private long windowHandle = MemoryUtil.NULL;
    private long glContext = MemoryUtil.NULL;
    private @NonNull String title = "Tribal Trouble";
    private boolean resized;
    private boolean closeRequested;
    private boolean active;
    private boolean iconified;
    private @Nullable LWJGL3InputProvider inputProvider;

    // Use realistic initial defaults to avoid assertion failures in UI code
    // and ensure scale calculation works correctly before window is created.
    private int cachedWidth = SerializableDisplayMode.MIN_WIDTH;
    private int cachedHeight = SerializableDisplayMode.MIN_HEIGHT;
    private int cachedLogicalWidth = SerializableDisplayMode.MIN_WIDTH;
    private int cachedLogicalHeight = SerializableDisplayMode.MIN_HEIGHT;

    public LWJGL3Window() {
        ensureSDL();
    }

    public void setInputProvider(@Nullable LWJGL3InputProvider inputProvider) {
        this.inputProvider = inputProvider;
    }

    private static void ensureSDL() {
        if (initialized.compareAndSet(false, true)) {
            if (!SDL_Init(SDL_INIT_VIDEO)) {
                initialized.set(false);
                throw new IllegalStateException("Unable to initialize SDL: " + SDL_GetError());
            }
        }
    }

    @Override
    public void create(@NonNull SerializableDisplayMode mode, boolean fullscreen) {
        logger.log(Level.INFO, "Creating window: " + mode + ", fullscreen: " + fullscreen);

        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        if (displayID == 0) displayID = SDL_GetPrimaryDisplay();

        float scale = getMonitorContentScale().x;
        Vector2f logical = getLogicalSize((int) (mode.getWidth() / scale), (int) (mode.getHeight() / scale));
        int width = (int) logical.x;
        int height = (int) logical.y;

        logger.log(Level.INFO, "Creating window with logical size: " + width + "x" + height + " (from physical: "
                + mode.getWidth() + "x" + mode.getHeight() + ")");

        if (windowHandle != MemoryUtil.NULL) {
            // Reconfigure existing window
            SDL_SetWindowSize(windowHandle, width, height);

            if (fullscreen) {
                // By passing NULL for the mode, SDL3 will automatically pick the best fullscreen 
                // mode that matches the window's current dimensions.
                if (!SDL_SetWindowFullscreenMode(windowHandle, null)) {
                    logger.log(Level.WARNING, "Failed to set fullscreen mode: " + SDL_GetError());
                }
            }

            if (!SDL_SetWindowFullscreen(windowHandle, fullscreen)) {
                logger.log(Level.WARNING, "Failed to set fullscreen state to " + fullscreen + ": " + SDL_GetError());
            }

            if (!fullscreen) {
                SDL_SetWindowPosition(windowHandle, SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED);
            }

            SDL_RaiseWindow(windowHandle);
            SDL_ShowWindow(windowHandle);
            syncViewport();

            // Fallback: if SDL reported 0, use the requested dimensions
            if (cachedWidth <= 0) cachedWidth = mode.getWidth();
            if (cachedHeight <= 0) cachedHeight = mode.getHeight();
            return;
        }

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 1);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG | (DEBUG
                ? SDL_GL_CONTEXT_DEBUG_FLAG : 0));

        SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24);
        SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
        SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 1);

        long flags = SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE | SDL_WINDOW_HIDDEN | SDL_WINDOW_HIGH_PIXEL_DENSITY;
        if (fullscreen) {
            flags |= SDL_WINDOW_FULLSCREEN;
        }

        windowHandle = SDL_CreateWindow(title, width, height, flags);
        if (windowHandle == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create the SDL window: " + SDL_GetError());
        }

        SDL_SetWindowMinimumSize(windowHandle, SerializableDisplayMode.MIN_WIDTH, SerializableDisplayMode.MIN_HEIGHT);

        if (!fullscreen) {
            SDL_SetWindowPosition(windowHandle, SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED);
        }

        glContext = SDL_GL_CreateContext(windowHandle);
        if (glContext == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create the OpenGL context: " + SDL_GetError());
        }

        SDL_GL_MakeCurrent(windowHandle, glContext);
        GL.createCapabilities();

        SDL_ShowWindow(windowHandle);
        SDL_RaiseWindow(windowHandle);

        // Initial state
        active = true;
        iconified = false;

        syncViewport();

        // Fallback: if SDL reported 0, use the requested dimensions
        if (cachedWidth <= 0) cachedWidth = mode.getWidth();
        if (cachedHeight <= 0) cachedHeight = mode.getHeight();
    }

    @Override
    public float getPixelDensity() {
        if (windowHandle != MemoryUtil.NULL) {
            return SDL_GetWindowPixelDensity(windowHandle);
        }
        int displayID = SDL_GetPrimaryDisplay();
        SDL_DisplayMode mode = SDL_GetDesktopDisplayMode(displayID);
        return (mode != null) ? mode.pixel_density() : 1.0f;
    }

    private @NonNull Vector2f getLogicalSize(int logicalWidth, int logicalHeight) {
        int width = logicalWidth;
        int height = logicalHeight;

        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect usableBounds = SDL_Rect.malloc(stack);
            if (SDL_GetDisplayUsableBounds(displayID, usableBounds)) {
                // Limit the window size to the usable bounds (screen size minus taskbar/menubar)
                if (width > usableBounds.w()) {
                    width = usableBounds.w();
                }
                if (height > usableBounds.h()) {
                    height = usableBounds.h();
                }
            }
        }
        return new Vector2f(width, height);
    }

    private void syncViewport() {
        if (windowHandle == MemoryUtil.NULL) return;
        updateCachedDimensions();
        Renderer.getRenderer().getRenderContext().setViewport(0, 0, cachedWidth, cachedHeight);
    }

    @Override
    public void close() {
        if (isMac) {
            setMacPresentationOptions(0);
        }
        if (glContext != MemoryUtil.NULL) {
            SDL_GL_DestroyContext(glContext);
            glContext = MemoryUtil.NULL;
        }
        if (windowHandle != MemoryUtil.NULL) {
            SDL_DestroyWindow(windowHandle);
            windowHandle = MemoryUtil.NULL;
        }
        if (initialized.compareAndSet(true, false)) {
            SDL_Quit();
        }
    }

    @Override
    public boolean isOpen() {
        return windowHandle != MemoryUtil.NULL;
    }

    @Override
    public void update() {
        SDL_GL_SwapWindow(windowHandle);
    }

    @Override
    public void pollEvents() {
        pollEvents(0);
    }

    public void pollEvents(int timeoutMs) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Event event = SDL_Event.malloc(stack);
            boolean hasEvent;
            if (timeoutMs > 0) {
                hasEvent = SDL_WaitEventTimeout(event, timeoutMs);
            } else {
                hasEvent = SDL_PollEvent(event);
            }

            while (hasEvent) {
                if (inputProvider != null) {
                    inputProvider.processEvent(event);
                }
                switch (event.type()) {
                    case SDL_EVENT_QUIT -> setCloseRequested(true);
                    case SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED, SDL_EVENT_WINDOW_RESIZED -> {
                        syncViewport();
                        resized = true;
                    }
                    case SDL_EVENT_WINDOW_FOCUS_GAINED -> active = true;
                    case SDL_EVENT_WINDOW_FOCUS_LOST -> active = false;
                    case SDL_EVENT_WINDOW_MINIMIZED -> iconified = true;
                    case SDL_EVENT_WINDOW_RESTORED -> iconified = false;
                    case SDL_EVENT_WINDOW_EXPOSED -> {
                        // On some platforms, window might be focused but hidden.
                        // Exposed means it's visible again.
                        if (iconified) iconified = false;
                    }
                }
                hasEvent = SDL_PollEvent(event);
            }
        }
    }

    @Override
    public boolean isCloseRequested() {
        boolean r = closeRequested;
        setCloseRequested(false);
        return r;
    }

    @Override
    public void setCloseRequested(boolean value) {
        closeRequested = value;
    }

    @Override
    public boolean isActive() {
        if (isMac) {
            return isMacAppActive() || active;
        }
        return active;
    }

    @Override
    public boolean isVisible() {
        return (SDL_GetWindowFlags(windowHandle) & SDL_WINDOW_HIDDEN) == 0;
    }

    @Override
    public boolean isIconified() {
        return iconified;
    }

    @Override
    public boolean wasResized() {
        boolean r = resized;
        resized = false;
        return r;
    }

    @Override
    public void setIcon(@NonNull Path imagePath) {
        if (windowHandle == MemoryUtil.NULL) return;

        if (isMac) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            ByteBuffer pixels = STBImage.stbi_load(imagePath.toString(), w, h, comp, 4);
            if (pixels == null) {
                System.err.println("Failed to load icon: " + imagePath + " Reason: " + STBImage.stbi_failure_reason());
                return;
            }

            SDL_Surface surface = SDL_CreateSurfaceFrom(w.get(0), h.get(0), SDL_PIXELFORMAT_ABGR8888, pixels, w.get(0)
                    * 4);
            if (surface != null) {
                SDL_SetWindowIcon(windowHandle, surface);
                SDL_DestroySurface(surface);
            }

            STBImage.stbi_image_free(pixels);
        }
    }

    @Override
    public void restore() {
        SDL_RestoreWindow(windowHandle);
    }

    @Override
    public void minimize() {
        SDL_MinimizeWindow(windowHandle);
    }

    @Override
    public void show() {
        SDL_ShowWindow(windowHandle);
    }

    @Override
    public void focus() {
        if (isMac) {
            // macOS focus fix: ensure events are flushed before raising window
            // https://github.com/libsdl-org/SDL/issues/13920
            try (MemoryStack stack = MemoryStack.stackPush()) {
                SDL_Event event = SDL_Event.malloc(stack);
                while (SDL_PollEvent(event)) {
                    if (inputProvider != null) {
                        inputProvider.processEvent(event);
                    }
                    // Handle critical window events during flush
                    switch (event.type()) {
                        case SDL_EVENT_QUIT -> setCloseRequested(true);
                        case SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> resized = true;
                    }
                }
            }
        }
        SDL_RaiseWindow(windowHandle);
    }

    private void updateCachedDimensions() {
        if (windowHandle == MemoryUtil.NULL) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            SDL_GetWindowSizeInPixels(windowHandle, w, h);
            int fw = w.get(0);
            int fh = h.get(0);
            if (fw > 0 && fh > 0) {
                cachedWidth = fw;
                cachedHeight = fh;
            }

            SDL_GetWindowSize(windowHandle, w, h);
            int lw = w.get(0);
            int lh = h.get(0);
            if (lw > 0 && lh > 0) {
                cachedLogicalWidth = lw;
                cachedLogicalHeight = lh;
            }
        }
    }

    @Override
    public int getWidth() {
        return cachedWidth;
    }

    @Override
    public int getHeight() {
        return cachedHeight;
    }

    @Override
    public int getLogicalWidth() {
        return cachedLogicalWidth;
    }

    @Override
    public int getLogicalHeight() {
        return cachedLogicalHeight;
    }

    @Override
    public void setTitle(@NonNull String title) {
        this.title = title;
        if (windowHandle != MemoryUtil.NULL) {
            SDL_SetWindowTitle(windowHandle, title);
        }
    }

    @Override
    public void setVSyncEnabled(boolean enabled) {
        SDL_GL_SetSwapInterval(enabled ? 1 : 0);
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
        if (windowHandle != MemoryUtil.NULL) {
            // Optimization: if we're just toggling the state for the same resolution
            if (!SDL_SetWindowFullscreen(windowHandle, fullscreen)) {
                logger.log(Level.WARNING, "Failed to toggle fullscreen: " + SDL_GetError());
            }
            return;
        }
        create(getDisplayMode(), fullscreen);
    }

    @Override
    public @NonNull List<@NonNull SerializableDisplayMode> getFullscreenDisplayModes() {
        return getAvailableDisplayModes();
    }

    @Override
    public @NonNull List<@NonNull SerializableDisplayMode> getWindowedDisplayModes() {
        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        if (displayID == 0) {
            return List.of();
        }

        int[][] standardResolutions = {
                {1024, 768}, {1280, 720}, {1280, 800}, {1280, 1024},
                {1440, 900}, {1600, 900}, {1600, 1200}, {1680, 1050},
                {1920, 1080}, {1920, 1200}, {2560, 1440}, {2560, 1600},
                {3440, 1440}, {3840, 2160}
        };

        float scale = getMonitorContentScale().x;
        int maxW = Integer.MAX_VALUE;
        int maxH = Integer.MAX_VALUE;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect usable = SDL_Rect.malloc(stack);
            if (SDL_GetDisplayUsableBounds(displayID, usable)) {
                maxW = usable.w();
                maxH = usable.h();
            }
        }

        final int finalMaxW = maxW;
        final int finalMaxH = maxH;
        var current = getDisplayMode();

        List<SerializableDisplayMode> modes = Arrays.stream(standardResolutions)
                .filter(res -> (res[0] / scale) <= finalMaxW && (res[1] / scale) <= finalMaxH)
                .map(res -> new SerializableDisplayMode(res[0], res[1], current.getBitsPerPixel(), current
                        .getFrequency()))
                .filter(SerializableDisplayMode::isModeValid)
                .collect(Collectors.toCollection(ArrayList::new));

        // Ensure current mode is included if it fits
        if (modes.stream().noneMatch(current::isEquivalent)) {
            if ((current.getWidth() / scale) <= finalMaxW && (current.getHeight() / scale) <= finalMaxH) {
                modes.add(current);
            }
        }

        return modes.stream()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private @NonNull Vector2f getMonitorPhysicalResolution(int displayID) {
        SDL_DisplayMode desktop = SDL_GetDesktopDisplayMode(displayID);
        if (desktop == null) {
            return new Vector2f(SerializableDisplayMode.MIN_WIDTH, SerializableDisplayMode.MIN_HEIGHT);
        }
        int maxW = (int) (desktop.w() * desktop.pixel_density());
        int maxH = (int) (desktop.h() * desktop.pixel_density());

        PointerBuffer pb = SDL_GetFullscreenDisplayModes(displayID);
        if (pb != null) {
            int n = pb.remaining();
            int nativeMaxW = 0;
            int nativeMaxH = 0;
            for (int i = 0; i < n; i++) {
                SDL_DisplayMode mode = SDL_DisplayMode.create(pb.get(i));
                if (mode.pixel_density() <= 1.0f) {
                    if (mode.w() > nativeMaxW) {
                        nativeMaxW = mode.w();
                    }
                    if (mode.h() > nativeMaxH) {
                        nativeMaxH = mode.h();
                    }
                }
            }
            nSDL_free(pb.address());
            if (nativeMaxW > 0 && nativeMaxH > 0) {
                maxW = nativeMaxW;
                maxH = nativeMaxH;
            }
        }
        return new Vector2f(maxW, maxH);
    }

    @Override
    public @NonNull List<@NonNull SerializableDisplayMode> getAvailableDisplayModes() {
        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        if (displayID == 0) {
            return List.of();
        }

        SDL_DisplayMode desktop = SDL_GetDesktopDisplayMode(displayID);
        if (desktop == null) {
            return List.of();
        }

        // On macOS, SDL3 display modes are returned in logical coordinates, not physical.
        // Multiplying by pixel_density causes runaway resolution limits.
        // We will treat the raw SDL_DisplayMode w/h as the target dimensions.
        int nativeMaxW = desktop.w();
        int nativeMaxH = desktop.h();

        PointerBuffer pb = SDL_GetFullscreenDisplayModes(displayID);
        if (pb != null) {
            int n = pb.remaining();
            for (int i = 0; i < n; i++) {
                SDL_DisplayMode mode = SDL_DisplayMode.create(pb.get(i));
                if (mode.w() > nativeMaxW) nativeMaxW = mode.w();
                if (mode.h() > nativeMaxH) nativeMaxH = mode.h();
            }
        }

        logger.info(String.format(Locale.ROOT, "Display Mode Discovery: Desktop=%dx%d, Density=%.2f, NativeMax=%dx%d",
                desktop.w(), desktop.h(), desktop.pixel_density(), nativeMaxW, nativeMaxH));

        // Key: width x height, Value: Best mode found for that resolution
        Map<String, SerializableDisplayMode> resolutionToBestMode = new HashMap<>();

        // Helper to process and potentially add a mode
        java.util.function.Consumer<SDL_DisplayMode> processMode = (mode) -> {
            int bpp = SDL_BITSPERPIXEL(mode.format());
            if (bpp == 24) bpp = 32;

            int width = mode.w();
            int height = mode.h();
            int freq = (int) mode.refresh_rate();

            SerializableDisplayMode sMode = new SerializableDisplayMode(width, height, bpp, freq);
            if (SerializableDisplayMode.isModeValid(sMode)) {
                String key = width + "x" + height;
                resolutionToBestMode.merge(key, sMode, (m1, m2) -> {
                    // Prefer higher frequency
                    if (m1.getFrequency() != m2.getFrequency()) {
                        return m1.getFrequency() > m2.getFrequency() ? m1 : m2;
                    }
                    // Then higher BPP
                    return m1.getBitsPerPixel() >= m2.getBitsPerPixel() ? m1 : m2;
                });
            }
        };

        // Add desktop mode
        processMode.accept(desktop);

        // Add fullscreen modes
        if (pb != null) {
            int n = pb.remaining();
            for (int i = 0; i < n; i++) {
                processMode.accept(SDL_DisplayMode.create(pb.get(i)));
            }
            nSDL_free(pb.address());
        }

        return resolutionToBestMode.values().stream()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    @Override
    public @NonNull SerializableDisplayMode getDisplayMode() {
        int width, height;

        if (windowHandle != MemoryUtil.NULL) {
            width = getWidth(); // Physical Pixels
            height = getHeight();
        } else {
            // Fallback default
            width = SerializableDisplayMode.MIN_WIDTH;
            height = SerializableDisplayMode.MIN_HEIGHT;
        }

        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();

        int freq = 60;
        int bpp = 32;
        SDL_DisplayMode mode = SDL_GetDesktopDisplayMode(displayID);
        if (mode != null) {
            freq = (int) mode.refresh_rate();
            bpp = SDL_BITSPERPIXEL(mode.format());
            if (bpp == 24) bpp = 32;

            if (windowHandle == MemoryUtil.NULL) {
                // Return dimensions directly without multiplying by density
                width = mode.w();
                height = mode.h();
            }
        }

        return new SerializableDisplayMode(width, height, bpp, freq);
    }

    @Override
    public void setDisplayMode(@NonNull SerializableDisplayMode mode) {
        create(mode, isFullscreen());
    }

    @Override
    public void makeCurrent() {
        SDL_GL_MakeCurrent(windowHandle, glContext);
    }

    public long getHandle() {
        return windowHandle;
    }

    @Override
    public boolean isFullscreen() {
        return windowHandle != MemoryUtil.NULL && (SDL_GetWindowFlags(windowHandle) & SDL_WINDOW_FULLSCREEN) != 0;
    }

    @Override
    public @NonNull Vector2f getMonitorPhysicalSize() {
        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        int props = SDL_GetDisplayProperties(displayID);
        // SDL 3 property keys for physical dimensions in millimeters
        long w = SDL_GetNumberProperty(props, "SDL.display.physical_width", 0);
        long h = SDL_GetNumberProperty(props, "SDL.display.physical_height", 0);
        return new Vector2f((float) w, (float) h);
    }

    @Override
    public @NonNull Vector2f getMonitorContentScale() {
        if (windowHandle != MemoryUtil.NULL) {
            float scale = SDL_GetWindowDisplayScale(windowHandle);
            return new Vector2f(scale, scale);
        }

        int displayID = SDL_GetPrimaryDisplay();
        float contentScale = SDL_GetDisplayContentScale(displayID);
        if (contentScale <= 0.0f) contentScale = 1.0f;

        return new Vector2f(contentScale, contentScale);
    }

    @Override
    public @NonNull Vector2f getWindowContentScale() {
        float scale = (windowHandle == MemoryUtil.NULL) ? 1.0f : SDL_GetWindowDisplayScale(windowHandle);
        return new Vector2f(scale, scale);
    }

    private @Nullable SDL_DisplayMode findMatchingDisplayMode(int displayID,
            @NonNull SerializableDisplayMode targetMode) {
        PointerBuffer pb = SDL_GetFullscreenDisplayModes(displayID);
        if (pb == null) {
            return null;
        }
        try {
            int n = pb.remaining();
            long bestMatchAddress = MemoryUtil.NULL;
            int bestFreqDiff = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                SDL_DisplayMode sdlMode = SDL_DisplayMode.create(pb.get(i));
                // Match against physical pixels
                int width = (int) (sdlMode.w() * sdlMode.pixel_density());
                int height = (int) (sdlMode.h() * sdlMode.pixel_density());
                if (width == targetMode.getWidth() && height == targetMode.getHeight()) {
                    int diff = Math.abs((int) sdlMode.refresh_rate() - targetMode.getFrequency());
                    if (diff < bestFreqDiff) {
                        bestFreqDiff = diff;
                        bestMatchAddress = pb.get(i);
                    }
                }
            }
            if (bestMatchAddress != MemoryUtil.NULL) {
                return SDL_DisplayMode.create(bestMatchAddress);
            }
            return null;
        } finally {
            nSDL_free(pb.address());
        }
    }

    private void setMacPresentationOptions(int options) {
        if (isMac) {
            try {
                initMacFFI();
                if (macInitialized && nsApp != MemoryUtil.NULL) {
                    JNI.invokePPPV(nsApp, setPresentationOptionsSel, (long) options, objc_msgSend);
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to set macOS presentation options", t);
            }
        }
    }
}
