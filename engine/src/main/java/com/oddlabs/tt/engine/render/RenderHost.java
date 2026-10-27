package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.base.resource.NativeResource;
import com.oddlabs.tt.base.util.StatCounter;
import com.oddlabs.tt.engine.render.state.GLRenderContext;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.engine.util.GLUtils;
import com.oddlabs.tt.engine.vbo.VBO;
import com.oddlabs.tt.window.SerializableDisplayMode;
import com.oddlabs.tt.window.Window;
import com.oddlabs.tt.window.WindowSettings;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Host and display manager for OpenGL rendering contexts, display passes, viewports, and frame metrics.
 */
public final class RenderHost implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(RenderHost.class.getSimpleName());

    private final StatCounter fps = new StatCounter(10);

    private final Window window;
    private final Settings settings;
    private final GLRenderContext renderContext = new GLRenderContext();

    private int lastDisplayW = -1;
    private int lastDisplayH = -1;

    public RenderHost(Window window, Settings settings) {
        this.window = window;
        this.settings = settings;
    }

    public float getFPS() {
        return fps.getAveragePerUpdate();
    }

    public GpuTimer getGpuTimer() {
        return renderContext.gpuTimer();
    }

    @Override
    public void close() {
        cleanup();
    }

    public void runWithContext(Runnable action) {
        ScopedValue.where(RenderContext.CURRENT, renderContext).run(action);
    }

    public void resetContext() {
        renderContext.reset();
    }

    public void display(Runnable renderPass) {
        fps.updateDelta(System.currentTimeMillis());
        NativeResource.processCleanupTasks();
        GLUtils.checkGLError("After Cleanup");

        int w = window.getWidth();
        int h = window.getHeight();
        if (w != lastDisplayW || h != lastDisplayH) {
            logger.info("[RenderHost] display() viewport changed: " + lastDisplayW + "x" + lastDisplayH + " -> " + w
                    + "x"
                    + h);
            lastDisplayW = w;
            lastDisplayH = h;
        }
        renderContext.setViewport(0, 0, w, h);
        int requestedSamples = WindowSettings.from(settings).view_samples;
        int effectiveSamples = computeEffectiveSamples(requestedSamples);
        renderContext.setEffectiveSamples(effectiveSamples);
        renderContext.setMultisampleEnabled(effectiveSamples > 1);

        renderPass.run();

        if (DebugFlags.debugRenderingEnabled()) {
            renderContext.validate();
        }
    }

    public void updateProgress(Runnable renderPass) {
        renderContext.reset(); // Fix texture bleeding
        display(renderPass);
        window.update();
        window.pollEvents();
    }

    public void cleanup() {
        logger.info("Cleaning up RenderHost...");
        renderContext.close();
        destroyNative();
        logger.fine("Native resources still registered: " + NativeResource.getCount());
        logger.info("RenderHost cleanup complete.");
    }

    private static void destroyNative() {
        logger.info("Clearing Resources...");
        Resources.clearResources();
        logger.info("RenderHost Closed.");
    }

    private void dumpWindowInfo() {
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

    public void initNative(boolean safeMode) throws Exception, Error {
        WindowSettings windowSettings = WindowSettings.from(settings);
        try {
            int bpp = 32;
            try {
                bpp = window.getDisplayMode().getBitsPerPixel();
            } catch (Exception _) {
                /* ignore */
            }

            window.setTitle("Tribal Trouble");

            SerializableDisplayMode target_mode;
            int width = safeMode ? windowSettings.view_width : windowSettings.new_view_width;
            int height = safeMode ? windowSettings.view_height : windowSettings.new_view_height;
            int freq = safeMode ? windowSettings.view_freq : windowSettings.new_view_freq;

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

            boolean fs = windowSettings.fullscreen;
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
            logger.info("Display DPI: " + String.format("%.1f", window.getDisplayDPI()));
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

        logger.info("vsync = " + windowSettings.vsync);
        window.setVSyncEnabled(windowSettings.vsync);
        NativeResource.setErrorChecker(GLUtils::checkGLError);
        initGL();
        window.update();
    }

    public int computeEffectiveSamples(int requestedSamples) {
        if (requestedSamples <= 1 || !DebugFlags.draw_msaa) {
            return 0;
        }
        if (window.getPixelDensity() > 1.0f) {
            return 0;
        }
        if (window.isExclusiveFullscreen()) {
            float dpi = window.getDisplayDPI();
            int height = window.getHeight();
            if (dpi > RenderConfig.MSAA_DPI_THRESHOLD_HIGH) {
                return 0;
            } else if (dpi >= RenderConfig.MSAA_DPI_THRESHOLD_LOW || height > RenderConfig.MSAA_MAX_HEIGHT_TIER1) {
                return Math.min(requestedSamples, 2);
            } else {
                return Math.min(requestedSamples, 4);
            }
        }
        return Math.min(requestedSamples, 4);
    }

    private void initGL() {
        VBO.releaseAll();
        int requestedSamples = WindowSettings.from(settings).view_samples;
        int effectiveSamples = computeEffectiveSamples(requestedSamples);
        renderContext.setEffectiveSamples(effectiveSamples);
        renderContext.applyDefaults(effectiveSamples > 1);
        int w = window.getWidth();
        int h = window.getHeight();
        logger.info("[RenderHost] initGL: window.getWidth()=" + w + ", window.getHeight()=" + h
                + ", displayDPI=" + String.format("%.1f", window.getDisplayDPI())
                + ", effectiveSamples=" + effectiveSamples);
        renderContext.setViewport(0, 0, w, h);
    }
}
