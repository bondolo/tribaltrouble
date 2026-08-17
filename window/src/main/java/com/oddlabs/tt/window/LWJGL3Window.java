package com.oddlabs.tt.window;

import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.macosx.MacOSXLibrary;
import org.lwjgl.system.macosx.ObjCRuntime;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO;
import static org.lwjgl.sdl.SDLInit.SDL_Init;
import static org.lwjgl.sdl.SDLInit.SDL_Quit;
import static org.lwjgl.sdl.SDLKeycode.SDL_KMOD_ALT;
import static org.lwjgl.sdl.SDLPixels.SDL_BITSPERPIXEL;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_ABGR8888;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_F11;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_ENTER;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RETURN;
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
    private static long nsApp = MemoryUtil.NULL;

    private static long cgMainDisplayID = MemoryUtil.NULL;
    private static long cgDisplayCopyAllDisplayModes = MemoryUtil.NULL;
    private static long cgDisplayModeGetIOFlags = MemoryUtil.NULL;
    private static long cgDisplayModeGetPixelWidth = MemoryUtil.NULL;
    private static long cgDisplayModeGetPixelHeight = MemoryUtil.NULL;
    private static long cfArrayGetCount = MemoryUtil.NULL;
    private static long cfArrayGetValueAtIndex = MemoryUtil.NULL;
    private static long cfRelease = MemoryUtil.NULL;

    private static boolean macInitialized = false;

    private static void initMacFFI() {
        if (isMac && !macInitialized) {
            try {
                objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                nsAppClass = ObjCRuntime.objc_getClass("NSApplication");
                sharedApplicationSel = ObjCRuntime.sel_getUid("sharedApplication");
                isActiveSel = ObjCRuntime.sel_getUid("isActive");
                nsApp = JNI.invokePPP(nsAppClass, sharedApplicationSel, objc_msgSend);

                var cg = MacOSXLibrary.create("/System/Library/Frameworks/CoreGraphics.framework");
                cgMainDisplayID = cg.getFunctionAddress("CGMainDisplayID");
                cgDisplayCopyAllDisplayModes = cg.getFunctionAddress("CGDisplayCopyAllDisplayModes");
                cgDisplayModeGetIOFlags = cg.getFunctionAddress("CGDisplayModeGetIOFlags");
                cgDisplayModeGetPixelWidth = cg.getFunctionAddress("CGDisplayModeGetPixelWidth");
                cgDisplayModeGetPixelHeight = cg.getFunctionAddress("CGDisplayModeGetPixelHeight");

                var cf = MacOSXLibrary.create("/System/Library/Frameworks/CoreFoundation.framework");
                cfArrayGetCount = cf.getFunctionAddress("CFArrayGetCount");
                cfArrayGetValueAtIndex = cf.getFunctionAddress("CFArrayGetValueAtIndex");
                cfRelease = cf.getFunctionAddress("CFRelease");

                macInitialized = true;
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to initialize macOS FFI caching", t);
            }
        }
    }

    private static @Nullable List<@NonNull SerializableDisplayMode> getMacNativeDisplayModes() {
        if (!isMac) return null;
        initMacFFI();
        if (!macInitialized) return null;

        try {
            int displayID = JNI.invokeI(cgMainDisplayID);
            long modesArray = JNI.invokePP(displayID, MemoryUtil.NULL, cgDisplayCopyAllDisplayModes);
            if (modesArray == MemoryUtil.NULL) return null;

            int count = (int) JNI.invokePJ(modesArray, cfArrayGetCount);
            List<SerializableDisplayMode> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                long mode = JNI.invokePPJ(modesArray, i, cfArrayGetValueAtIndex);
                if (mode == MemoryUtil.NULL) continue;

                int flags = JNI.invokePI(mode, cgDisplayModeGetIOFlags);
                boolean isUsableForDesktop = (flags & 0x00000004) != 0;
                boolean isSafeForHardware = (flags & 0x00000008) != 0;
                boolean isStretched = (flags & 0x04000000) != 0;
                boolean isInterlaced = (flags & 0x00000020) != 0;
                boolean isTelevisionOutput = (flags & 0x00200000) != 0;

                if (!isUsableForDesktop || !isSafeForHardware || isStretched || isInterlaced || isTelevisionOutput) {
                    continue;
                }

                int w = (int) JNI.invokePJ(mode, cgDisplayModeGetPixelWidth);
                int h = (int) JNI.invokePJ(mode, cgDisplayModeGetPixelHeight);
                if (w >= SerializableDisplayMode.MIN_WIDTH && h >= SerializableDisplayMode.MIN_HEIGHT) {
                    result.add(new SerializableDisplayMode(w, h, 32, 60));
                }
            }
            JNI.invokePV(modesArray, cfRelease);
            return result;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to query CoreGraphics display modes via FFI", t);
            return null;
        }
    }

    private static boolean isMacApplicationActive() {
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
    private @Nullable WindowEventListener eventListener;
    private boolean pendingFullscreenRestore;
    private @Nullable SerializableDisplayMode lastCreatedMode;

    private static final int RESTORE_NONE = 0;
    private static final int RESTORE_LEAVING_FULLSCREEN = 1;
    private static final int RESTORE_UNMINIMIZING = 2;

    private int restoreState = RESTORE_NONE;
    private boolean lastMacAppActive;

    private @NonNull WindowSettings settings;

    private int cachedWidth = SerializableDisplayMode.MIN_WIDTH;
    private int cachedHeight = SerializableDisplayMode.MIN_HEIGHT;
    private int cachedLogicalWidth = SerializableDisplayMode.MIN_WIDTH;
    private int cachedLogicalHeight = SerializableDisplayMode.MIN_HEIGHT;

    public LWJGL3Window() {
        this(new WindowSettings());
    }

    public LWJGL3Window(@NonNull WindowSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        ensureSDL();
    }

    @Override
    public @NonNull WindowSettings getSettings() {
        return settings;
    }

    @Override
    public void setSettings(@NonNull WindowSettings settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    @Override
    public void setEventListener(@Nullable WindowEventListener listener) {
        this.eventListener = listener;
    }

    private static void ensureSDL() {
        if (initialized.compareAndSet(false, true)) {
            if (!SDL_Init(SDL_INIT_VIDEO)) {
                initialized.set(false);
                throw new IllegalStateException("Unable to initialize SDL: " + SDL_GetError());
            }
        }
    }

    private @Nullable SDL_DisplayMode findMatchingDisplayMode(@NonNull MemoryStack stack, int displayID,
            @NonNull SerializableDisplayMode targetMode) {
        PointerBuffer pb = SDL_GetFullscreenDisplayModes(displayID);
        if (pb == null) {
            return null;
        }
        SDL_DisplayMode bestMatch = null;
        int targetW = targetMode.getWidth();
        int targetH = targetMode.getHeight();
        int targetFreq = targetMode.getFrequency();

        int n = pb.remaining();
        for (int i = 0; i < n; i++) {
            SDL_DisplayMode mode = SDL_DisplayMode.create(pb.get(i));
            int modePhysW = Math.round(mode.w() * mode.pixel_density());
            int modePhysH = Math.round(mode.h() * mode.pixel_density());
            if (modePhysW == targetW && modePhysH == targetH) {
                if (bestMatch == null) {
                    bestMatch = mode;
                } else {
                    double currentDiff = Math.abs(bestMatch.refresh_rate() - targetFreq);
                    double newDiff = Math.abs(mode.refresh_rate() - targetFreq);
                    if (newDiff < currentDiff) {
                        bestMatch = mode;
                    } else if (newDiff == currentDiff) {
                        if (mode.refresh_rate() > bestMatch.refresh_rate()) {
                            bestMatch = mode;
                        }
                    }
                }
            }
        }

        if (bestMatch != null) {
            logger.info("findMatchingDisplayMode: matched " + targetW + "x" + targetH
                    + " -> SDL mode " + bestMatch.w() + "x" + bestMatch.h()
                    + " density=" + bestMatch.pixel_density()
                    + " freq=" + bestMatch.refresh_rate());
        } else {
            logger.warning("findMatchingDisplayMode: no SDL fullscreen mode found for " + targetW + "x" + targetH);
        }

        SDL_DisplayMode result = null;
        if (bestMatch != null) {
            result = SDL_DisplayMode.malloc(stack).set(bestMatch);
        }

        nSDL_free(pb.address());
        return result;
    }

    @Override
    public void create(@NonNull SerializableDisplayMode mode, boolean fullscreen) {
        logger.log(Level.INFO, "Creating window: " + mode + ", fullscreen: " + fullscreen);
        this.lastCreatedMode = mode;

        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        if (displayID == 0) displayID = SDL_GetPrimaryDisplay();

        SDL_DisplayMode desktop = SDL_GetDesktopDisplayMode(displayID);
        int maxW = desktop != null ? (int) (desktop.w() * desktop.pixel_density()) : 3840;
        int maxH = desktop != null ? (int) (desktop.h() * desktop.pixel_density()) : 2160;

        PointerBuffer pb = SDL_GetFullscreenDisplayModes(displayID);
        if (pb != null) {
            int n = pb.remaining();
            for (int i = 0; i < n; i++) {
                SDL_DisplayMode dm = SDL_DisplayMode.create(pb.get(i));
                int physW = (int) (dm.w() * dm.pixel_density());
                int physH = (int) (dm.h() * dm.pixel_density());
                if (physW > maxW) maxW = physW;
                if (physH > maxH) maxH = physH;
            }
            nSDL_free(pb.address());
        }

        if (mode.getWidth() > maxW || mode.getHeight() > maxH) {
            logger.warning(String.format("Requested mode %dx%d exceeds maximum display resolution %dx%d. Clamping.",
                    mode.getWidth(), mode.getHeight(), maxW, maxH));
            mode = new SerializableDisplayMode(maxW, maxH, mode.getBitsPerPixel(), mode.getFrequency());
        }

        if (windowHandle == MemoryUtil.NULL) {
            createWindow(mode, fullscreen);
        } else {
            try {
                setDisplayMode(mode);
                setFullscreen(fullscreen);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to update existing window, recreating", e);
                destroy();
                createWindow(mode, fullscreen);
            }
        }
        updateCachedDimensions();
        this.active = true;
        this.iconified = false;
    }

    private void createWindow(@NonNull SerializableDisplayMode mode, boolean fullscreen) {
        long flags = SDL_WINDOW_OPENGL | SDL_WINDOW_HIGH_PIXEL_DENSITY | SDL_WINDOW_RESIZABLE;
        if (fullscreen) {
            flags |= SDL_WINDOW_FULLSCREEN;
        }

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 1);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG);
        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
        SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24);
        SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 1);

        float density = getPixelDensity();
        if (settings.view_samples > 0 && density <= 1.0f) {
            SDL_GL_SetAttribute(SDL_GL_MULTISAMPLEBUFFERS, 1);
            SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, settings.view_samples);
        } else {
            SDL_GL_SetAttribute(SDL_GL_MULTISAMPLEBUFFERS, 0);
            SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, 0);
        }

        Vector2f logical = getLogicalSize(mode.getWidth(), mode.getHeight());
        windowHandle = SDL_CreateWindow(title, (int) logical.x, (int) logical.y, flags);
        if (windowHandle == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create SDL window: " + SDL_GetError());
        }

        if (fullscreen) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                int displayID = SDL_GetDisplayForWindow(windowHandle);
                if (displayID == 0) displayID = SDL_GetPrimaryDisplay();
                SDL_DisplayMode match = findMatchingDisplayMode(stack, displayID, mode);
                if (match != null) {
                    SDL_SetWindowFullscreenMode(windowHandle, match);
                    logger.info("createWindow: applied fullscreen mode " + match.w() + "x" + match.h() + " density="
                            + match.pixel_density() + " freq=" + match.refresh_rate());
                } else {
                    SDL_SetWindowFullscreenMode(windowHandle, null);
                    logger.info("createWindow: set borderless desktop fullscreen for mode " + mode);
                }
            }
        }

        glContext = SDL_GL_CreateContext(windowHandle);
        if (glContext == MemoryUtil.NULL) {
            SDL_DestroyWindow(windowHandle);
            windowHandle = MemoryUtil.NULL;
            throw new IllegalStateException("Failed to create OpenGL context: " + SDL_GetError());
        }

        try {
            makeCurrent();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to make context current", e);
        }

        GL.createCapabilities();
        logger.info("OpenGL Context Created: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));

        updateCachedDimensions();
        GL11.glViewport(0, 0, cachedWidth, cachedHeight);
        if (eventListener != null) {
            eventListener.onResized(cachedWidth, cachedHeight);
        }
    }

    private void destroy() {
        if (glContext != MemoryUtil.NULL) {
            SDL_GL_DestroyContext(glContext);
            glContext = MemoryUtil.NULL;
        }
        if (windowHandle != MemoryUtil.NULL) {
            SDL_DestroyWindow(windowHandle);
            windowHandle = MemoryUtil.NULL;
        }
    }

    @Override
    public void close() {
        destroy();
        if (initialized.compareAndSet(true, false)) {
            SDL_Quit();
        }
    }

    @Override
    public void update() {
        if (windowHandle != MemoryUtil.NULL && glContext != MemoryUtil.NULL) {
            SDL_GL_SwapWindow(windowHandle);
        }
    }

    public long getHandle() {
        return windowHandle;
    }

    private void updateCachedDimensions() {
        if (windowHandle == MemoryUtil.NULL) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);

            SDL_GetWindowSizeInPixels(windowHandle, w, h);
            cachedWidth = Math.max(1, w.get(0));
            cachedHeight = Math.max(1, h.get(0));

            SDL_GetWindowSize(windowHandle, w, h);
            cachedLogicalWidth = Math.max(1, w.get(0));
            cachedLogicalHeight = Math.max(1, h.get(0));
        }
    }

    public void syncViewport() {
        if (windowHandle == MemoryUtil.NULL) return;
        updateCachedDimensions();
        GL11.glViewport(0, 0, cachedWidth, cachedHeight);
        if (eventListener != null) {
            eventListener.onResized(cachedWidth, cachedHeight);
        }
    }

    @Override
    public void pollEvents() {
        pollEvents(0);
    }

    public void pollEvents(int timeoutMs) {
        if (windowHandle == MemoryUtil.NULL) {
            return;
        }
        resized = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Event event = SDL_Event.malloc(stack);

            boolean hasEvents = false;
            if (timeoutMs > 0) {
                if (SDL_WaitEventTimeout(event, timeoutMs)) {
                    hasEvents = true;
                }
            }

            while (hasEvents || SDL_PollEvent(event)) {
                hasEvents = false; // Reset so next iterations use PollEvent

                int type = event.type();
                switch (type) {
                    case SDL_EVENT_QUIT -> closeRequested = true;
                    case SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED, SDL_EVENT_WINDOW_RESIZED -> {
                        int prevWidth = cachedWidth;
                        int prevHeight = cachedHeight;
                        updateCachedDimensions();
                        if (cachedWidth != prevWidth || cachedHeight != prevHeight) {
                            resized = true;
                            logger.info("[Window] Resized: Framebuffer " + cachedWidth + "x" + cachedHeight
                                    + " (Logical: " + cachedLogicalWidth + "x" + cachedLogicalHeight + ")");
                            GL11.glViewport(0, 0, cachedWidth, cachedHeight);
                            if (eventListener != null) {
                                eventListener.onResized(cachedWidth, cachedHeight);
                            }
                        }
                    }
                    case SDL_EVENT_WINDOW_SHOWN, SDL_EVENT_WINDOW_RESTORED -> {
                        logger.info("MacNative: Window shown/restored event received.");
                        iconified = false;
                        active = true;
                        if (pendingFullscreenRestore) {
                            logger.info("MacNative: Pending restore active. Re-applying fullscreen.");
                            pendingFullscreenRestore = false;
                            if (eventListener != null) {
                                eventListener.onFocusGained();
                            }
                        }
                    }
                    case SDL_EVENT_WINDOW_MINIMIZED -> {
                        logger.info("MacNative: Window minimized event received.");
                        iconified = true;
                        active = false;
                    }
                    case SDL_EVENT_WINDOW_OCCLUDED -> {
                        logger.info("MacNative: Window occluded event received.");
                        active = false;
                    }
                    case SDL_EVENT_WINDOW_FOCUS_GAINED -> {
                        active = true;
                        if (settings.fullscreen) {
                            long flags = SDL_GetWindowFlags(windowHandle);
                            boolean isMinimized = (flags & SDL_WINDOW_MINIMIZED) != 0;
                            if (isMinimized) {
                                logger.info("MacNative: Focus gained while minimized. Triggering restore.");
                                restore();
                            }
                        }
                        if (eventListener != null) {
                            eventListener.onFocusGained();
                        }
                    }
                    case SDL_EVENT_WINDOW_FOCUS_LOST -> {
                        active = false;
                    }
                    case SDL_EVENT_KEY_DOWN -> {
                        SDL_KeyboardEvent keyEvent = event.key();
                        int scancode = keyEvent.scancode();
                        int mod = keyEvent.mod();
                        if (scancode == SDL_SCANCODE_F11 ||
                                ((scancode == SDL_SCANCODE_RETURN || scancode == SDL_SCANCODE_KP_ENTER) && (mod
                                        & SDL_KMOD_ALT) != 0)) {
                            if (eventListener != null) {
                                eventListener.onToggleFullscreen();
                            }
                        }
                    }
                }

                if (eventListener != null) {
                    eventListener.handleSDLEvent(event);
                }
            }
        }
    }

    @Override
    public boolean isOpen() {
        return windowHandle != MemoryUtil.NULL;
    }

    @Override
    public boolean isCloseRequested() {
        return closeRequested;
    }

    @Override
    public void setCloseRequested(boolean value) {
        this.closeRequested = value;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isVisible() {
        if (windowHandle == MemoryUtil.NULL) return false;
        long flags = SDL_GetWindowFlags(windowHandle);
        return (flags & SDL_WINDOW_HIDDEN) == 0;
    }

    @Override
    public boolean isIconified() {
        if (windowHandle == MemoryUtil.NULL) return false;
        long flags = SDL_GetWindowFlags(windowHandle);
        return (flags & SDL_WINDOW_MINIMIZED) != 0 || iconified;
    }

    @Override
    public boolean isMaximized() {
        if (windowHandle == MemoryUtil.NULL) return false;
        long flags = SDL_GetWindowFlags(windowHandle);
        return (flags & SDL_WINDOW_MAXIMIZED) != 0;
    }

    @Override
    public boolean wasResized() {
        return resized;
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
    public void setTitle(String title) {
        this.title = title != null ? title : "Tribal Trouble";
        if (windowHandle != MemoryUtil.NULL) {
            SDL_SetWindowTitle(windowHandle, this.title);
        }
    }

    @Override
    public void setVSyncEnabled(boolean enabled) {
        if (windowHandle != MemoryUtil.NULL && glContext != MemoryUtil.NULL) {
            SDL_GL_SetSwapInterval(enabled ? 1 : 0);
        }
    }

    @Override
    public void setFullscreen(boolean fullscreen) throws Exception {
        if (windowHandle == MemoryUtil.NULL) return;

        boolean isCurrentlyFs = isFullscreen();
        if (isCurrentlyFs == fullscreen) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (fullscreen) {
                int displayID = SDL_GetDisplayForWindow(windowHandle);
                if (displayID == 0) displayID = SDL_GetPrimaryDisplay();

                SerializableDisplayMode targetMode = getDisplayMode();
                SDL_DisplayMode match = findMatchingDisplayMode(stack, displayID, targetMode);

                if (match != null) {
                    SDL_SetWindowFullscreenMode(windowHandle, match);
                    logger.info("setFullscreen: applied exclusive mode " + match.w() + "x" + match.h() + " density="
                            + match.pixel_density() + " freq=" + match.refresh_rate());
                } else {
                    SDL_SetWindowFullscreenMode(windowHandle, null);
                    logger.info("setFullscreen: set borderless desktop fullscreen for mode " + targetMode);
                }
            } else {
                SDL_SetWindowFullscreenMode(windowHandle, null);
            }

            if (!SDL_SetWindowFullscreen(windowHandle, fullscreen)) {
                throw new IllegalStateException("Failed to toggle fullscreen: " + SDL_GetError());
            }

            if (!fullscreen) {
                SDL_SetWindowBordered(windowHandle, true);
                Vector2f logical = getLogicalSize(settings.view_width, settings.view_height);
                SDL_SetWindowSize(windowHandle, (int) logical.x, (int) logical.y);
                SDL_SetWindowPosition(windowHandle, SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED);
            }

            updateCachedDimensions();
            GL11.glViewport(0, 0, cachedWidth, cachedHeight);
            if (eventListener != null) {
                eventListener.onResized(cachedWidth, cachedHeight);
            }
        }
    }

    @Override
    public @NonNull List<@NonNull SerializableDisplayMode> getFullscreenDisplayModes() {
        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        if (displayID == 0) displayID = SDL_GetPrimaryDisplay();

        List<SerializableDisplayMode> macNativeModes = getMacNativeDisplayModes();
        if (macNativeModes != null && !macNativeModes.isEmpty()) {
            return macNativeModes.stream()
                    .distinct()
                    .sorted((m1, m2) -> {
                        int cmp = Integer.compare(m2.getWidth(), m1.getWidth());
                        if (cmp != 0) return cmp;
                        return Integer.compare(m2.getHeight(), m1.getHeight());
                    })
                    .collect(Collectors.toList());
        }

        PointerBuffer pb = SDL_GetFullscreenDisplayModes(displayID);
        if (pb == null) {
            return List.of();
        }

        List<SerializableDisplayMode> modes = new ArrayList<>();
        int n = pb.remaining();
        for (int i = 0; i < n; i++) {
            SDL_DisplayMode dm = SDL_DisplayMode.create(pb.get(i));
            int bpp = SDL_BITSPERPIXEL(dm.format());
            float density = dm.pixel_density();
            int physW = Math.round(dm.w() * density);
            int physH = Math.round(dm.h() * density);

            if (physW >= SerializableDisplayMode.MIN_WIDTH && physH >= SerializableDisplayMode.MIN_HEIGHT) {
                modes.add(new SerializableDisplayMode(physW, physH, bpp, (int) dm.refresh_rate()));
            }
        }
        nSDL_free(pb.address());

        return modes.stream()
                .distinct()
                .sorted((m1, m2) -> {
                    int cmp = Integer.compare(m2.getWidth(), m1.getWidth());
                    if (cmp != 0) return cmp;
                    return Integer.compare(m2.getHeight(), m1.getHeight());
                })
                .collect(Collectors.toList());
    }

    @Override
    public @NonNull List<@NonNull SerializableDisplayMode> getWindowedDisplayModes() {
        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        if (displayID == 0) displayID = SDL_GetPrimaryDisplay();

        int maxW = 3840;
        int maxH = 2160;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect rect = SDL_Rect.malloc(stack);
            if (SDL_GetDisplayUsableBounds(displayID, rect)) {
                float density = getPixelDensity();
                maxW = (int) (rect.w() * density);
                maxH = (int) (rect.h() * density);
            }
        }

        final int limitW = maxW;
        final int limitH = maxH;

        return Arrays.stream(SerializableDisplayMode.WINDOWED_PRESETS)
                .filter(mode -> mode.getWidth() <= limitW && mode.getHeight() <= limitH)
                .sorted(Comparator.comparingInt(SerializableDisplayMode::getWidth).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public @NonNull SerializableDisplayMode getDisplayMode() {
        if (lastCreatedMode != null) {
            return lastCreatedMode;
        }
        return new SerializableDisplayMode(cachedWidth, cachedHeight, 32, 60);
    }

    @Override
    public void setDisplayMode(@NonNull SerializableDisplayMode mode) throws Exception {
        this.lastCreatedMode = mode;
        if (windowHandle == MemoryUtil.NULL) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (isFullscreen()) {
                int displayID = SDL_GetDisplayForWindow(windowHandle);
                if (displayID == 0) displayID = SDL_GetPrimaryDisplay();
                SDL_DisplayMode match = findMatchingDisplayMode(stack, displayID, mode);
                if (match != null) {
                    SDL_SetWindowFullscreenMode(windowHandle, match);
                    SDL_SyncWindow(windowHandle);
                    logger.info("setDisplayMode: switched fullscreen to " + match.w() + "x" + match.h() + " density="
                            + match.pixel_density() + " freq=" + match.refresh_rate());
                } else {
                    SDL_SetWindowFullscreenMode(windowHandle, null);
                    SDL_SyncWindow(windowHandle);
                    logger.info("setDisplayMode: switched to borderless desktop fullscreen for mode " + mode);
                }
            } else {
                Vector2f logical = getLogicalSize(mode.getWidth(), mode.getHeight());
                SDL_SetWindowSize(windowHandle, (int) logical.x, (int) logical.y);
                SDL_SetWindowPosition(windowHandle, SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED);
                SDL_SyncWindow(windowHandle);
            }
            updateCachedDimensions();
            GL11.glViewport(0, 0, cachedWidth, cachedHeight);
            if (eventListener != null) {
                eventListener.onResized(cachedWidth, cachedHeight);
            }
        }
    }

    @Override
    public void setIcon(Path imagePath) {
        if (windowHandle == MemoryUtil.NULL || imagePath == null) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            ByteBuffer image = STBImage.stbi_load(imagePath.toString(), w, h, comp, 4);
            if (image == null) {
                logger.warning("Failed to load icon image: " + STBImage.stbi_failure_reason());
                return;
            }

            SDL_Surface surface = SDL_CreateSurfaceFrom(w.get(0), h.get(0), SDL_PIXELFORMAT_ABGR8888, image, w.get(0)
                    * 4);
            if (surface != null) {
                SDL_SetWindowIcon(windowHandle, surface);
                SDL_DestroySurface(surface);
            }
            STBImage.stbi_image_free(image);
        }
    }

    @Override
    public void restore() {
        if (windowHandle != MemoryUtil.NULL) {
            SDL_RestoreWindow(windowHandle);
        }
    }

    @Override
    public void minimize() {
        if (windowHandle != MemoryUtil.NULL) {
            SDL_MinimizeWindow(windowHandle);
        }
    }

    @Override
    public void show() {
        if (windowHandle != MemoryUtil.NULL) {
            SDL_ShowWindow(windowHandle);
        }
    }

    @Override
    public void focus() {
        if (windowHandle != MemoryUtil.NULL) {
            SDL_RaiseWindow(windowHandle);
        }
    }

    @Override
    public void makeCurrent() throws Exception {
        if (windowHandle != MemoryUtil.NULL && glContext != MemoryUtil.NULL) {
            if (!SDL_GL_MakeCurrent(windowHandle, glContext)) {
                throw new IllegalStateException("Failed to make OpenGL context current: " + SDL_GetError());
            }
        }
    }

    @Override
    public boolean isFullscreen() {
        if (windowHandle == MemoryUtil.NULL) return false;
        long flags = SDL_GetWindowFlags(windowHandle);
        return (flags & SDL_WINDOW_FULLSCREEN) != 0;
    }

    @Override
    public @NonNull Vector2f getMonitorPhysicalSize() {
        return new Vector2f(500, 300);
    }

    @Override
    public @NonNull Vector2f getMonitorContentScale() {
        float density = getPixelDensity();
        return new Vector2f(density, density);
    }

    @Override
    public @NonNull Vector2f getWindowContentScale() {
        float density = getPixelDensity();
        return new Vector2f(density, density);
    }

    @Override
    public float getPixelDensity() {
        if (windowHandle == MemoryUtil.NULL) return 1.0f;
        float density = SDL_GetWindowPixelDensity(windowHandle);
        return density > 0f ? density : 1.0f;
    }

    @Override
    public boolean isExclusiveFullscreenMode(@NonNull SerializableDisplayMode mode) {
        if (windowHandle == MemoryUtil.NULL) return false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int displayID = SDL_GetDisplayForWindow(windowHandle);
            if (displayID == 0) displayID = SDL_GetPrimaryDisplay();
            return findMatchingDisplayMode(stack, displayID, mode) != null;
        }
    }

    @Override
    public boolean isExclusiveFullscreen() {
        if (windowHandle == MemoryUtil.NULL) return false;
        return isFullscreen() && SDL_GetWindowFullscreenMode(windowHandle) != null;
    }

    @Override
    public void updateSystemUI(boolean playing) {
    }

    private Vector2f getLogicalSize(int physW, int physH) {
        float density = getPixelDensity();
        return new Vector2f(physW / density, physH / density);
    }
}
