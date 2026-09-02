package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.base.resource.NativeResource;
import com.oddlabs.tt.base.util.StatCounter;
import com.oddlabs.tt.engine.render.state.GLRenderContext;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.settings.Settings;
import com.oddlabs.tt.engine.util.GLUtils;
import com.oddlabs.tt.engine.vbo.VBO;
import com.oddlabs.tt.window.SerializableDisplayMode;
import com.oddlabs.tt.window.Window;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenGL graphics renderer and display manager.
 * Manages rendering contexts, display passes, viewports, VBO cleanup, and frame metrics.
 */
public final class Renderer implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(Renderer.class.getSimpleName());

    private static @Nullable Renderer renderer_instance;

    private static final StatCounter fps = new StatCounter(10);
    private static int num_triangles_rendered;
    private static volatile boolean finished = false;

    private final Window window;
    private final Settings settings;
    private final GLRenderContext renderContext = new GLRenderContext();

    private int lastDisplayW = -1;
    private int lastDisplayH = -1;

    public Renderer(Window window, Settings settings) {
        this.window = window;
        this.settings = settings;
        renderer_instance = this;
    }

    public static float getFPS() {
        return fps.getAveragePerUpdate();
    }

    public static boolean isRegistered() {
        return true;
    }

    public static void makeCurrent() {
        try {
            Renderer renderer = getRenderer();
            if (renderer != null) {
                renderer.getWindow().makeCurrent();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to make OpenGL context current", e);
        }
    }

    @Override
    public void close() {
        cleanup();
    }

    public Window getWindow() {
        return window;
    }

    public static @Nullable Renderer getRenderer() {
        return renderer_instance;
    }

    public Settings getSettings() {
        return settings;
    }

    public GLRenderContext getRenderContext() {
        return renderContext;
    }

    public static void registerTrianglesRendered(int count) {
        num_triangles_rendered += count;
    }

    public static int getTrianglesRendered() {
        return num_triangles_rendered;
    }

    public void display(FrameDriver driver) {
        num_triangles_rendered = 0;
        fps.updateDelta(System.currentTimeMillis());
        NativeResource.processCleanupTasks();
        GLUtils.checkGLError("After Cleanup");

        int w = window.getWidth();
        int h = window.getHeight();
        if (w != lastDisplayW || h != lastDisplayH) {
            logger.info("[Renderer] display() viewport changed: " + lastDisplayW + "x" + lastDisplayH + " -> " + w + "x"
                    + h);
            lastDisplayW = w;
            lastDisplayH = h;
        }
        renderContext.setViewport(0, 0, w, h);

        driver.render();

        if (DebugFlags.debugRenderingEnabled()) {
            renderContext.validate();
        }
    }

    public void updateProgress(FrameDriver driver) {
        renderContext.reset(); // Fix texture bleeding
        display(driver);
        window.update();
        window.pollEvents();
    }

    public static void shutdown() {
        finished = true;
    }

    public static boolean isFinished() {
        return finished;
    }

    public void cleanup() {
        logger.info("Cleaning up Renderer...");
        destroyNative();
        logger.fine("Native resources still registered: " + NativeResource.getCount());
        logger.info("Renderer cleanup complete.");
    }

    private static void destroyNative() {
        logger.info("Clearing Resources...");
        Resources.clearResources();
        logger.info("Renderer Closed.");
    }

    public static void dumpWindowInfo() {
        try {
            GLUtils.checkGLError("Pre-dumpWindowInfo");
            int r = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE);
            int g = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE);
            int b = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE);
            int a = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE);
            int depth = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE);
            int stencil = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE);
            logger.info("Window Info: r=" + r + " g=" + g + " b=" + b + " a=" + a + " depth=" + depth + " stencil="
                    + stencil);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to dump window info", e);
        }
    }

    public void initNative(boolean crashed) throws Exception {
        try {
            int bpp = 32;
            try {
                bpp = window.getDisplayMode().getBitsPerPixel();
            } catch (Exception _) {
                /* ignore */
            }

            window.setTitle("Tribal Trouble");

            SerializableDisplayMode target_mode;
            int width = crashed ? settings.window.view_width : settings.window.new_view_width;
            int height = crashed ? settings.window.view_height : settings.window.new_view_height;
            int freq = crashed ? settings.window.view_freq : settings.window.new_view_freq;

            if (width < SerializableDisplayMode.MIN_WIDTH || height < SerializableDisplayMode.MIN_HEIGHT) {
                try {
                    var modes = window.getFullscreenDisplayModes();
                    target_mode = modes.isEmpty()
                            ? new SerializableDisplayMode(SerializableDisplayMode.MIN_WIDTH,
                                    SerializableDisplayMode.MIN_HEIGHT, 32, 60)
                            : modes.getFirst();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to get available modes for default selection", e);
                    target_mode = new SerializableDisplayMode(SerializableDisplayMode.MIN_WIDTH,
                            SerializableDisplayMode.MIN_HEIGHT, 32, 60);
                }
            } else {
                target_mode = new SerializableDisplayMode(width, height, bpp, freq);
            }

            boolean fs = settings.window.fullscreen;
            window.create(target_mode, fs);

            Path iconPath = Path.of("assets/widget/TribalTrouble.wdgt/Icon.png");
            if (!Files.exists(iconPath)) {
                iconPath = Path.of("../assets/widget/TribalTrouble.wdgt/Icon.png");
            }
            logger.info("Setting icon from: " + iconPath.toAbsolutePath());
            window.setIcon(iconPath);

            var physSize = window.getMonitorPhysicalSize();
            logger.info("Monitor Physical Size: " + (int) physSize.x() + "mm x " + (int) physSize.y() + "mm");
            var monScale = window.getMonitorContentScale();
            logger.info("Monitor Content Scale: " + monScale.x() + "x, " + monScale.y() + "y");
            var winScale = window.getWindowContentScale();
            logger.info("Window Content Scale: " + winScale.x() + "x, " + winScale.y() + "y");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OpenGL Failure", e);
            throw e;
        }

        String version = GL11.glGetString(GL11.GL_VERSION);
        logger.info("GL version: '" + version + "'");
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        logger.info("GL vendor: '" + vendor + "'");
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        logger.info("GL renderer: '" + renderer + "'");

        renderContext.init();
        dumpWindowInfo();

        int num_combined_tex_units = GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        logger.info("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS: " + num_combined_tex_units);
        if (num_combined_tex_units < 8) {
            throw new IllegalStateException("Number of combined texture image units " + num_combined_tex_units
                    + " is less than the required 8.");
        }

        logger.info("vsync = " + settings.window.vsync);
        if (settings.window.vsync) {
            window.setVSyncEnabled(true);
        }
        NativeResource.setErrorChecker(GLUtils::checkGLError);
        initGL();
        initVisibleGL();
    }

    private void initVisibleGL() {
        window.update();
    }

    public static void initGL() {
        Renderer renderer = getRenderer();
        if (renderer != null) {
            RenderContext context = renderer.renderContext;
            VBO.releaseAll(context);
            context.applyDefaults();
            Window window = renderer.window;
            int w = window.getWidth();
            int h = window.getHeight();
            logger.info("[Renderer] initGL: window.getWidth()=" + w + ", window.getHeight()=" + h);
            context.setViewport(0, 0, w, h);
        }
    }

    public static void clearScreen() {
        GL11.glClearColor(Color.Linear.BLACK.r(), Color.Linear.BLACK.g(), Color.Linear.BLACK.b(), Color.Linear.BLACK
                .a());
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }
}
