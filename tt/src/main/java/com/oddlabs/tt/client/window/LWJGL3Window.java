package com.oddlabs.tt.client.window;

import com.oddlabs.tt.core.global.Settings;
import com.oddlabs.tt.client.input.LWJGL3InputProvider;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SerializableDisplayMode;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.JNI;
import org.lwjgl.sdl.SDLProperties;
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

    /**
     * Physical dimensions (width and height) in pixels.
     */
    private record PhysicalResolution(int width, int height) {
    }

    private @NonNull PhysicalResolution getNativePhysicalResolution(int sdlDisplayID) {
        if (!isMac) {
            return new PhysicalResolution(0, 0);
        }
        initMacFFI();
        if (!macInitialized) {
            return new PhysicalResolution(0, 0);
        }

        int nativeDisplayID = 0;
        if (windowHandle != MemoryUtil.NULL) {
            try {
                long nswindow = SDLProperties.SDL_GetPointerProperty(
                        SDL_GetWindowProperties(windowHandle),
                        "SDL.window.cocoa.window",
                        MemoryUtil.NULL
                );
                if (nswindow != MemoryUtil.NULL) {
                    long screen = JNI.invokePPP(nswindow, ObjCRuntime.sel_getUid("screen"), objc_msgSend);
                    if (screen != MemoryUtil.NULL) {
                        long deviceDesc = JNI.invokePPP(screen, ObjCRuntime.sel_getUid("deviceDescription"),
                                objc_msgSend);
                        if (deviceDesc != MemoryUtil.NULL) {
                            long nsStringClass = ObjCRuntime.objc_getClass("NSString");
                            long stringWithUTF8StringSel = ObjCRuntime.sel_getUid("stringWithUTF8String:");
                            long keyString;
                            try (MemoryStack stack = MemoryStack.stackPush()) {
                                keyString = JNI.invokePPPP(nsStringClass, stringWithUTF8StringSel, MemoryUtil
                                        .memAddress(stack.UTF8("NSScreenNumber")), objc_msgSend);
                            }
                            if (keyString != MemoryUtil.NULL) {
                                long screenNumberVal = JNI.invokePPPP(deviceDesc, ObjCRuntime.sel_getUid(
                                        "objectForKey:"), keyString, objc_msgSend);
                                if (screenNumberVal != MemoryUtil.NULL) {
                                    nativeDisplayID = JNI.invokePPI(screenNumberVal, ObjCRuntime.sel_getUid(
                                            "unsignedIntValue"), objc_msgSend);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to retrieve native CGDirectDisplayID from NSWindow", t);
            }
        }

        if (nativeDisplayID == 0) {
            try {
                if (cgMainDisplayID != MemoryUtil.NULL) {
                    nativeDisplayID = (int) JNI.invokeP(cgMainDisplayID);
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to invoke CGMainDisplayID", t);
            }
        }

        if (nativeDisplayID == 0) {
            return new PhysicalResolution(0, 0);
        }

        int nativeW = 0;
        int nativeH = 0;

        try {
            long modesArray = JNI.invokePPP((long) nativeDisplayID, MemoryUtil.NULL, cgDisplayCopyAllDisplayModes);
            if (modesArray != MemoryUtil.NULL) {
                long count = JNI.invokePP(modesArray, cfArrayGetCount);
                for (long i = 0; i < count; i++) {
                    long mode = JNI.invokePPP(modesArray, i, cfArrayGetValueAtIndex);
                    if (mode != MemoryUtil.NULL) {
                        int flags = (int) JNI.invokePP(mode, cgDisplayModeGetIOFlags);
                        if ((flags & 0x02000000) != 0) { // kDisplayModeNativeFlag = 0x02000000
                            nativeW = (int) JNI.invokePP(mode, cgDisplayModeGetPixelWidth);
                            nativeH = (int) JNI.invokePP(mode, cgDisplayModeGetPixelHeight);
                            break;
                        }
                    }
                }
                JNI.invokePP(modesArray, cfRelease);
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to retrieve physical dimensions from CoreGraphics display modes", t);
        }

        return new PhysicalResolution(nativeW, nativeH);
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
    private boolean pendingFullscreenRestore;
    private @Nullable SerializableDisplayMode lastCreatedMode;

    private static final int RESTORE_NONE = 0;
    private static final int RESTORE_LEAVING_FULLSCREEN = 1;
    private static final int RESTORE_UNMINIMIZING = 2;

    private int restoreState = RESTORE_NONE;
    private boolean lastMacAppActive;

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

    private @Nullable SDL_DisplayMode findMatchingDisplayMode(@NonNull MemoryStack stack, int displayID,
            @NonNull SerializableDisplayMode targetMode) {
        PointerBuffer pb = SDL_GetFullscreenDisplayModes(displayID);
        if (pb == null) {
            return null;
        }
        SDL_DisplayMode bestMatch = null;
        // SerializableDisplayMode stores physical pixel dimensions; SDL_DisplayMode reports logical
        // points. Convert each SDL mode to physical pixels before comparing.
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
            int nativeMaxW = 0;
            int nativeMaxH = 0;
            for (int i = 0; i < n; i++) {
                SDL_DisplayMode sdlMode = SDL_DisplayMode.create(pb.get(i));
                if (sdlMode.w() > nativeMaxW) nativeMaxW = sdlMode.w();
                if (sdlMode.h() > nativeMaxH) nativeMaxH = sdlMode.h();
            }
            nSDL_free(pb.address());
            if (nativeMaxW > 0 && nativeMaxH > 0) {
                maxW = nativeMaxW;
                maxH = nativeMaxH;
            }
        }

        int physicalW = mode.getWidth();
        int physicalH = mode.getHeight();
        if (physicalW > maxW || physicalH > maxH) {
            physicalW = maxW;
            physicalH = maxH;
            mode = new SerializableDisplayMode(physicalW, physicalH, mode.getBitsPerPixel(), mode.getFrequency());
            logger.log(Level.INFO, "Capped requested mode to desktop size: " + mode);
        }

        int width, height;
        if (fullscreen) {
            width = mode.getWidth();
            height = mode.getHeight();
        } else {
            Vector2f logical = getLogicalSize(mode.getWidth(), mode.getHeight());
            width = (int) logical.x;
            height = (int) logical.y;
        }

        logger.log(Level.INFO, "Creating window with logical size: " + width + "x" + height + " (from mode: "
                + mode.getWidth() + "x" + mode.getHeight() + ")");

        if (windowHandle != MemoryUtil.NULL) {
            if (fullscreen) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    SDL_DisplayMode sdlMode = findMatchingDisplayMode(stack, displayID, mode);
                    if (sdlMode != null) {
                        if (!SDL_SetWindowFullscreenMode(windowHandle, sdlMode)) {
                            logger.log(Level.WARNING, "Failed to set fullscreen mode: " + SDL_GetError());
                        }
                    } else {
                        SDL_SetWindowFullscreenMode(windowHandle, null);
                    }
                }
            } else {
                SDL_SetWindowFullscreenMode(windowHandle, null);
            }

            if (!SDL_SetWindowFullscreen(windowHandle, fullscreen)) {
                logger.log(Level.WARNING, "Failed to set fullscreen state to " + fullscreen + ": " + SDL_GetError());
            }

            if (!fullscreen) {
                SDL_SetWindowBordered(windowHandle, true);
                SDL_SetWindowSize(windowHandle, width, height);
                SDL_SetWindowPosition(windowHandle, SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED);
            }

            SDL_RaiseWindow(windowHandle);
            SDL_ShowWindow(windowHandle);
            SDL_SyncWindow(windowHandle);
            syncViewport();

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

        Settings settings = Renderer.getRenderer().getSettings();
        float density = getPixelDensity();
        if (settings.view_samples > 0 && density <= 1.0f) {
            SDL_GL_SetAttribute(SDL_GL_MULTISAMPLEBUFFERS, 1);
            SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, settings.view_samples);
        } else {
            SDL_GL_SetAttribute(SDL_GL_MULTISAMPLEBUFFERS, 0);
            SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, 0);
        }

        long flags = SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE | SDL_WINDOW_HIDDEN | SDL_WINDOW_HIGH_PIXEL_DENSITY;

        windowHandle = SDL_CreateWindow(title, width, height, flags);
        if (windowHandle == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create the SDL window: " + SDL_GetError());
        }

        if (fullscreen) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                SDL_DisplayMode sdlMode = findMatchingDisplayMode(stack, displayID, mode);
                if (sdlMode != null) {
                    if (!SDL_SetWindowFullscreenMode(windowHandle, sdlMode)) {
                        logger.log(Level.WARNING, "Failed to set fullscreen mode: " + SDL_GetError());
                    }
                } else {
                    SDL_SetWindowFullscreenMode(windowHandle, null);
                }
            }
            if (!SDL_SetWindowFullscreen(windowHandle, true)) {
                logger.log(Level.WARNING, "Failed to set fullscreen state to true: " + SDL_GetError());
            }
        }

        SDL_SetWindowMinimumSize(windowHandle, SerializableDisplayMode.MIN_WIDTH, SerializableDisplayMode.MIN_HEIGHT);

        if (!fullscreen) {
            SDL_SetWindowPosition(windowHandle, SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED);
        }

        glContext = SDL_GL_CreateContext(windowHandle);
        if (glContext == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create the OpenGL context: " + SDL_GetError());
        }

        if (!SDL_GL_MakeCurrent(windowHandle, glContext)) {
            throw new IllegalStateException("Failed to make the OpenGL context current: " + SDL_GetError());
        }
        GL.createCapabilities();

        SDL_ShowWindow(windowHandle);
        SDL_RaiseWindow(windowHandle);
        SDL_SyncWindow(windowHandle);

        active = true;
        iconified = false;

        syncViewport();

        if (cachedWidth <= 0) cachedWidth = mode.getWidth();
        if (cachedHeight <= 0) cachedHeight = mode.getHeight();
    }

    @Override
    public float getPixelDensity() {
        if (windowHandle != MemoryUtil.NULL) {
            if (SDL_GetWindowFullscreenMode(windowHandle) != null) {
                return 1.0f;
            }
            long flags = SDL_GetWindowFlags(windowHandle);
            if ((flags & SDL_WINDOW_MINIMIZED) == 0) {
                float density = SDL_GetWindowPixelDensity(windowHandle);
                if (density > 0.0f) {
                    return density;
                }
            }
            int displayID = SDL_GetDisplayForWindow(windowHandle);
            if (displayID == 0) displayID = SDL_GetPrimaryDisplay();
            SDL_DisplayMode mode = SDL_GetDesktopDisplayMode(displayID);
            return (mode != null) ? mode.pixel_density() : 1.0f;
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

    private void checkPendingMinimize() {
    }

    private void checkPendingRestore() {
        if (pendingFullscreenRestore && windowHandle != MemoryUtil.NULL) {
            SerializableDisplayMode mode = lastCreatedMode != null ? lastCreatedMode : getDisplayMode();
            int displayID = SDL_GetDisplayForWindow(windowHandle);
            if (displayID == 0) displayID = SDL_GetPrimaryDisplay();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                SDL_DisplayMode sdlMode = findMatchingDisplayMode(stack, displayID, mode);
                if (sdlMode != null) {
                    if (!SDL_SetWindowFullscreenMode(windowHandle, sdlMode)) {
                        logger.log(Level.WARNING, "Failed to set fullscreen mode on restore: " + SDL_GetError());
                    }
                } else {
                    SDL_SetWindowFullscreenMode(windowHandle, null);
                }
            }
            resizeToFullscreenLogicalSize();

            if (SDL_SetWindowFullscreen(windowHandle, true)) {
                SDL_SyncWindow(windowHandle);
                syncViewport();
                resized = true;
                pendingFullscreenRestore = false;
                try {
                    Renderer.getLocalInput().getPointerInput().reapplyCursor();
                } catch (Exception ignored) {
                    // Safe to ignore if called before game has fully initialized
                }
            } else {
                logger.log(Level.FINE, "Failed to restore fullscreen state, will retry next frame: " + SDL_GetError());
            }
        }
    }

    @Override
    public void pollEvents() {
        pollEvents(0);
    }

    public void pollEvents(int timeoutMs) {
        if (isMac && windowHandle != MemoryUtil.NULL) {
            boolean currentMacActive = isMacAppActive();
            if (currentMacActive && !lastMacAppActive) {
                logger.info("MacNative: Application became active.");
                long flags = SDL_GetWindowFlags(windowHandle);
                boolean isMinimized = (flags & SDL_WINDOW_MINIMIZED) != 0;
                if (isMinimized && restoreState == RESTORE_NONE) {
                    if ((flags & SDL_WINDOW_FULLSCREEN) != 0) {
                        logger.info("MacNative: Fullscreen window minimized. Toggling fullscreen to false first.");
                        restoreState = RESTORE_LEAVING_FULLSCREEN;
                        if (!SDL_SetWindowFullscreen(windowHandle, false)) {
                            logger.warning("MacNative: SDL_SetWindowFullscreen(false) failed: " + SDL_GetError());
                            restoreState = RESTORE_NONE;
                        }
                    } else {
                        logger.info("MacNative: Windowed window minimized. Restoring directly.");
                        if (Renderer.getRenderer().getSettings().fullscreen) {
                            resizeToFullscreenLogicalSize();
                            pendingFullscreenRestore = true;
                        }
                        if (!SDL_RestoreWindow(windowHandle)) {
                            logger.warning("MacNative: SDL_RestoreWindow failed: " + SDL_GetError());
                        }
                    }
                }
            }
            lastMacAppActive = currentMacActive;
        }

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
                    case SDL_EVENT_WINDOW_RESIZED,
                            SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED,
                            SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED -> {
                        syncViewport();
                        resized = true;
                    }
                    case SDL_EVENT_WINDOW_FOCUS_GAINED -> {
                        active = true;
                        if (Renderer.getRenderer().getSettings().fullscreen) {
                            long flags = SDL_GetWindowFlags(windowHandle);
                            boolean isMinimized = (flags & SDL_WINDOW_MINIMIZED) != 0;
                            if (isMinimized && restoreState == RESTORE_NONE) {
                                logger.info(
                                        "Focus Gained: Fullscreen window minimized. Toggling fullscreen to false first.");
                                restoreState = RESTORE_LEAVING_FULLSCREEN;
                                if (!SDL_SetWindowFullscreen(windowHandle, false)) {
                                    logger.warning("Failed to exit fullscreen for restore: " + SDL_GetError());
                                    restoreState = RESTORE_NONE;
                                }
                            } else if (!isMinimized && SDL_GetWindowFullscreenMode(windowHandle) == null && restoreState
                                    == RESTORE_NONE) {
                                        logger.info(
                                                "Focus Gained: Fullscreen window restored but exclusive mode not active. Setting fullscreen.");
                                        pendingFullscreenRestore = true;
                                    }
                        }
                        if (isFullscreen() && !isMac) {
                            SDL_SetWindowBordered(windowHandle, false);
                        }
                        try {
                            Renderer.getLocalInput().getPointerInput().reapplyCursor();
                        } catch (Exception ignored) {
                            // Safe to ignore if called before game has fully initialized
                        }
                    }
                    case SDL_EVENT_WINDOW_FOCUS_LOST -> {
                        active = false;
                        boolean isExclusive = SDL_GetWindowFullscreenMode(windowHandle) != null;
                        if (isExclusive) {
                            SDL_MinimizeWindow(windowHandle);
                        }
                    }
                    case SDL_EVENT_WINDOW_MINIMIZED -> iconified = true;
                    case SDL_EVENT_WINDOW_RESTORED -> {
                        iconified = false;
                        if (restoreState == RESTORE_UNMINIMIZING) {
                            logger.info("Window Restored: Re-entering fullscreen.");
                            restoreState = RESTORE_NONE;
                            pendingFullscreenRestore = true;
                        } else if (Renderer.getRenderer().getSettings().fullscreen && SDL_GetWindowFullscreenMode(
                                windowHandle) == null && restoreState
                                        == RESTORE_NONE) {
                                            pendingFullscreenRestore = true;
                                        }
                        if (isFullscreen()) {
                            if (!isMac) {
                                SDL_SetWindowBordered(windowHandle, false);
                            }
                        } else {
                            SDL_SetWindowBordered(windowHandle, true);
                        }
                        syncViewport();
                        resized = true;
                    }
                    case SDL_EVENT_WINDOW_MAXIMIZED -> {
                        if (!isMac && !isFullscreen()) {
                            SDL_SetWindowBordered(windowHandle, false);
                        }
                        syncViewport();
                        resized = true;
                    }
                    case SDL_EVENT_WINDOW_ENTER_FULLSCREEN -> {
                        if (!isMac) {
                            SDL_SetWindowBordered(windowHandle, false);
                        }
                        syncViewport();
                        resized = true;
                    }
                    case SDL_EVENT_WINDOW_LEAVE_FULLSCREEN -> {
                        if (restoreState == RESTORE_LEAVING_FULLSCREEN) {
                            logger.info("Left Fullscreen: Unminimizing window.");
                            restoreState = RESTORE_UNMINIMIZING;
                            resizeToFullscreenLogicalSize();
                            if (!SDL_RestoreWindow(windowHandle)) {
                                logger.warning("Failed to restore window after leaving fullscreen: " + SDL_GetError());
                                restoreState = RESTORE_NONE;
                            }
                        } else {
                            SDL_SetWindowBordered(windowHandle, true);
                        }
                        syncViewport();
                        resized = true;
                    }
                    case SDL_EVENT_WINDOW_EXPOSED -> {
                        if (iconified) iconified = false;
                    }
                    case SDL_EVENT_KEY_DOWN -> {
                        SDL_KeyboardEvent keyEvent = event.key();
                        int scancode = keyEvent.scancode();
                        int mod = keyEvent.mod();
                        if (scancode == SDL_SCANCODE_F11 ||
                                ((scancode == SDL_SCANCODE_RETURN || scancode == SDL_SCANCODE_KP_ENTER) && (mod
                                        & SDL_KMOD_ALT) != 0)) {
                            Renderer.getRenderer().toggleFullscreen();
                        }
                    }
                    case SDL_EVENT_MOUSE_MOTION -> {
                        if (!isMac && !isFullscreen() && (SDL_GetWindowFlags(windowHandle) & SDL_WINDOW_MAXIMIZED)
                                != 0) {
                            float my = event.motion().y();
                            if (my < 5.0f) {
                                SDL_SetWindowBordered(windowHandle, true);
                            } else if (my > 30.0f) {
                                SDL_SetWindowBordered(windowHandle, false);
                            }
                        }
                    }
                }
                hasEvent = SDL_PollEvent(event);
            }
        }
        checkPendingMinimize();
        checkPendingRestore();
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
    public boolean isMaximized() {
        return windowHandle != MemoryUtil.NULL && (SDL_GetWindowFlags(windowHandle) & SDL_WINDOW_MAXIMIZED) != 0;
    }

    @Override
    public boolean wasResized() {
        boolean r = resized;
        if (r) {
            logger.info("[LWJGL3Window] wasResized() returning true and resetting");
        }
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
        if (Renderer.getRenderer().getSettings().fullscreen) {
            long flags = SDL_GetWindowFlags(windowHandle);
            if ((flags & SDL_WINDOW_MINIMIZED) != 0 && restoreState == RESTORE_NONE) {
                logger.info("restore() called: Toggling fullscreen to false first.");
                restoreState = RESTORE_LEAVING_FULLSCREEN;
                if (!SDL_SetWindowFullscreen(windowHandle, false)) {
                    logger.warning("Failed to exit fullscreen for restore: " + SDL_GetError());
                    restoreState = RESTORE_NONE;
                    SDL_RestoreWindow(windowHandle);
                }
                return;
            }
        }
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
            try (MemoryStack stack = MemoryStack.stackPush()) {
                SDL_Event event = SDL_Event.malloc(stack);
                while (SDL_PollEvent(event)) {
                    if (inputProvider != null) {
                        inputProvider.processEvent(event);
                    }
                    switch (event.type()) {
                        case SDL_EVENT_QUIT -> setCloseRequested(true);
                        case SDL_EVENT_WINDOW_RESIZED,
                                SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED,
                                SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED -> {
                            syncViewport();
                            resized = true;
                        }
                        case SDL_EVENT_WINDOW_FOCUS_GAINED -> {
                            active = true;
                            try {
                                Renderer.getLocalInput().getPointerInput().reapplyCursor();
                            } catch (Exception ignored) {
                                // Safe to ignore if called before game has fully initialized
                            }
                        }
                        case SDL_EVENT_WINDOW_FOCUS_LOST -> active = false;
                        case SDL_EVENT_WINDOW_MINIMIZED -> iconified = true;
                        case SDL_EVENT_WINDOW_RESTORED -> {
                            iconified = false;
                            if (restoreState == RESTORE_UNMINIMIZING) {
                                restoreState = RESTORE_NONE;
                                pendingFullscreenRestore = true;
                            } else if (Renderer.getRenderer().getSettings().fullscreen && SDL_GetWindowFullscreenMode(
                                    windowHandle) == null
                                    && restoreState == RESTORE_NONE) {
                                        pendingFullscreenRestore = true;
                                    }
                        }
                        case SDL_EVENT_WINDOW_LEAVE_FULLSCREEN -> {
                            if (restoreState == RESTORE_LEAVING_FULLSCREEN) {
                                restoreState = RESTORE_UNMINIMIZING;
                                resizeToFullscreenLogicalSize();
                                if (!SDL_RestoreWindow(windowHandle)) {
                                    restoreState = RESTORE_NONE;
                                }
                            }
                        }
                    }
                }
            }
        }
        SDL_RaiseWindow(windowHandle);
    }

    private void updateCachedDimensions() {
        if (windowHandle == MemoryUtil.NULL) return;
        int prevWidth = cachedWidth;
        int prevHeight = cachedHeight;
        int prevLogicalWidth = cachedLogicalWidth;
        int prevLogicalHeight = cachedLogicalHeight;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (SDL_GetWindowFullscreenMode(windowHandle) != null) {
                SDL_DisplayMode mode = SDL_GetWindowFullscreenMode(windowHandle);
                cachedWidth = mode.w();
                cachedHeight = mode.h();
                cachedLogicalWidth = mode.w();
                cachedLogicalHeight = mode.h();
            } else {
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
    }

    private void resizeToFullscreenLogicalSize() {
        if (windowHandle == MemoryUtil.NULL) return;
        SerializableDisplayMode mode = lastCreatedMode != null ? lastCreatedMode : getDisplayMode();
        float scale = getMonitorContentScale().x;
        int w = (int) (mode.getWidth() / scale);
        int h = (int) (mode.getHeight() / scale);
        SDL_SetWindowSize(windowHandle, w, h);
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
            if (!fullscreen) {
                pendingFullscreenRestore = false;
            }
            if (fullscreen) {
                SerializableDisplayMode currentMode = getDisplayMode();
                this.lastCreatedMode = currentMode;
                int displayID = SDL_GetDisplayForWindow(windowHandle);
                if (displayID == 0) displayID = SDL_GetPrimaryDisplay();
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    SDL_DisplayMode sdlMode = findMatchingDisplayMode(stack, displayID, currentMode);
                    if (sdlMode != null) {
                        if (!SDL_SetWindowFullscreenMode(windowHandle, sdlMode)) {
                            logger.log(Level.WARNING, "Failed to set fullscreen mode: " + SDL_GetError());
                        }
                    } else {
                        SDL_SetWindowFullscreenMode(windowHandle, null);
                    }
                }
            } else {
                SDL_SetWindowFullscreenMode(windowHandle, null);
            }

            if (!SDL_SetWindowFullscreen(windowHandle, fullscreen)) {
                logger.log(Level.WARNING, "Failed to toggle fullscreen: " + SDL_GetError());
            }

            if (!fullscreen) {
                SDL_SetWindowBordered(windowHandle, true);
                Settings settings = Renderer.getRenderer().getSettings();
                Vector2f logical = getLogicalSize(settings.view_width, settings.view_height);
                SDL_SetWindowSize(windowHandle, (int) logical.x, (int) logical.y);
                SDL_SetWindowPosition(windowHandle, SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED);
            }
            SDL_SyncWindow(windowHandle);
            syncViewport();
            resized = true;
            return;
        }
        create(getDisplayMode(), fullscreen);
    }

    @Override
    public @NonNull List<@NonNull SerializableDisplayMode> getFullscreenDisplayModes() {
        int displayID = windowHandle != MemoryUtil.NULL ? SDL_GetDisplayForWindow(windowHandle)
                : SDL_GetPrimaryDisplay();
        if (displayID == 0) {
            return List.of();
        }

        SDL_DisplayMode desktop = SDL_GetDesktopDisplayMode(displayID);
        if (desktop == null) {
            return List.of();
        }

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
        PhysicalResolution nativeRes = getNativePhysicalResolution(displayID);
        logger.info(String.format(Locale.ROOT,
                "Display Mode Discovery: Desktop=%dx%d, Density=%.2f, NativeMax=%dx%d, NativePhysical=%dx%d",
                desktop.w(), desktop.h(), desktop.pixel_density(), nativeMaxW, nativeMaxH, nativeRes.width(), nativeRes
                        .height()));

        Map<String, SerializableDisplayMode> resolutionToBestMode = new HashMap<>();

        java.util.function.Consumer<SDL_DisplayMode> processMode = (mode) -> {
            int bpp = SDL_BITSPERPIXEL(mode.format());
            if (bpp == 24) bpp = 32;

            // Store physical pixel dimensions to be consistent with create(), getDisplayMode(),
            // and settings, which all use physical pixels from SDL_GetWindowSizeInPixels.
            int width = Math.round(mode.w() * mode.pixel_density());
            int height = Math.round(mode.h() * mode.pixel_density());

            if (nativeRes.width() > 0 && nativeRes.height() > 0) {
                if (width > nativeRes.width() || height > nativeRes.height()) {
                    return;
                }
            }

            int freq = (int) mode.refresh_rate();

            SerializableDisplayMode sMode = new SerializableDisplayMode(width, height, bpp, freq);
            if (SerializableDisplayMode.isModeValid(sMode)) {
                String key = width + "x" + height;
                resolutionToBestMode.merge(key, sMode, (m1, m2) -> {
                    if (m1.getFrequency() != m2.getFrequency()) {
                        return m1.getFrequency() > m2.getFrequency() ? m1 : m2;
                    }
                    return m1.getBitsPerPixel() >= m2.getBitsPerPixel() ? m1 : m2;
                });
            }
        };

        // First pass: Only process modes with pixel_density == 1.0f (true physical modes)
        java.util.function.Consumer<SDL_DisplayMode> processModeFiltered = (mode) -> {
            if (mode.pixel_density() == 1.0f) {
                processMode.accept(mode);
            }
        };

        processModeFiltered.accept(desktop);

        if (pb != null) {
            int n = pb.remaining();
            for (int i = 0; i < n; i++) {
                processModeFiltered.accept(SDL_DisplayMode.create(pb.get(i)));
            }
        }

        // Fallback: If no 1.0 density modes found, process all modes
        if (resolutionToBestMode.isEmpty()) {
            logger.info(
                    "No exclusive fullscreen modes with pixel_density == 1.0f discovered. Falling back to all modes.");
            processMode.accept(desktop);
            if (pb != null) {
                int n = pb.remaining();
                for (int i = 0; i < n; i++) {
                    processMode.accept(SDL_DisplayMode.create(pb.get(i)));
                }
            }
        }

        if (pb != null) {
            nSDL_free(pb.address());
        }

        return resolutionToBestMode.values().stream()
                .sorted(Comparator.reverseOrder())
                .toList();
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
                .filter(res -> res[0] < (finalMaxW - 10) && res[1] < (finalMaxH - 40))
                .map(res -> new SerializableDisplayMode(res[0], res[1], current.getBitsPerPixel(), current
                        .getFrequency()))
                .filter(SerializableDisplayMode::isModeValid)
                .collect(Collectors.toCollection(ArrayList::new));

        if (modes.stream().noneMatch(current::isEquivalent)) {
            modes.add(current);
        }

        return modes.stream()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    @Override
    public @NonNull SerializableDisplayMode getDisplayMode() {
        int width, height;

        if (windowHandle != MemoryUtil.NULL) {
            boolean isExclusive = SDL_GetWindowFullscreenMode(windowHandle) != null;
            if (isExclusive) {
                width = getWidth();
                height = getHeight();
            } else {
                width = getLogicalWidth();
                height = getLogicalHeight();
            }
        } else {
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
        if (!SDL_GL_MakeCurrent(windowHandle, glContext)) {
            logger.log(Level.SEVERE, "Failed to make the OpenGL context current: " + SDL_GetError());
        }
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
        // SDL 3 does not directly expose monitor physical dimensions in mm.
        return new Vector2f(0, 0);
    }

    @Override
    public @NonNull Vector2f getMonitorContentScale() {
        int displayID = 0;
        if (windowHandle != MemoryUtil.NULL) {
            if (SDL_GetWindowFullscreenMode(windowHandle) != null) {
                return new Vector2f(1.0f, 1.0f);
            }
            long flags = SDL_GetWindowFlags(windowHandle);
            boolean minimized = (flags & SDL_WINDOW_MINIMIZED) != 0;
            if (!minimized) {
                float scale = SDL_GetWindowDisplayScale(windowHandle);
                if (scale > 0.0f) {
                    return new Vector2f(scale, scale);
                }
            }
            displayID = SDL_GetDisplayForWindow(windowHandle);
        }
        if (displayID == 0) {
            displayID = SDL_GetPrimaryDisplay();
        }
        SDL_DisplayMode mode = SDL_GetDesktopDisplayMode(displayID);
        float contentScale = (mode != null) ? mode.pixel_density() : 1.0f;
        if (contentScale <= 0.0f) contentScale = 1.0f;
        return new Vector2f(contentScale, contentScale);
    }

    @Override
    public @NonNull Vector2f getWindowContentScale() {
        float scale = (windowHandle == MemoryUtil.NULL) ? 1.0f : SDL_GetWindowDisplayScale(windowHandle);
        return new Vector2f(scale, scale);
    }

    @Override
    public boolean isExclusiveFullscreenMode(@NonNull SerializableDisplayMode mode) {
        if (windowHandle == MemoryUtil.NULL) {
            return false;
        }
        int displayID = SDL_GetDisplayForWindow(windowHandle);
        if (displayID == 0) {
            displayID = SDL_GetPrimaryDisplay();
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return findMatchingDisplayMode(stack, displayID, mode) != null;
        }
    }

    @Override
    public boolean isExclusiveFullscreen() {
        return windowHandle != MemoryUtil.NULL && SDL_GetWindowFullscreenMode(windowHandle) != null;
    }

    private long lastMacOptions = -1;
    private static long setPresentationOptionsSel = MemoryUtil.NULL;

    private void setMacPresentationOptions(long options) {
        if (isMac && nsApp != MemoryUtil.NULL && objc_msgSend != MemoryUtil.NULL) {
            try {
                if (setPresentationOptionsSel == MemoryUtil.NULL) {
                    setPresentationOptionsSel = ObjCRuntime.sel_getUid("setPresentationOptions:");
                }
                JNI.invokePPPP(nsApp, setPresentationOptionsSel, options, objc_msgSend);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to set macOS presentation options", t);
            }
        }
    }

    @Override
    public void updateSystemUI(boolean playing) {
        if (!isMac) {
            return;
        }
        long options = 0;
        if (isActive() && isFullscreen()) {
            boolean isExclusive = SDL_GetWindowFullscreenMode(windowHandle) != null;
            if (isExclusive) {
                options = 10; // NSApplicationPresentationHideDock | NSApplicationPresentationHideMenuBar
            } else {
                options = playing ? 10 : 5; // Hide completely when playing, Auto-Hide (5) at menu
            }
        }
        if (options != lastMacOptions) {
            setMacPresentationOptions(options);
            lastMacOptions = options;
        }
    }
}
