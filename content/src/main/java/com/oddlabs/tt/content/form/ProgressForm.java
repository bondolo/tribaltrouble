package com.oddlabs.tt.content.form;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.base.util.LoadCallback;
import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.camera.NullCamera;
import com.oddlabs.tt.client.delegate.CameraDelegate;
import com.oddlabs.tt.client.delegate.NullDelegate;
import com.oddlabs.tt.client.screen.Screen;
import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.ProgressBar;
import org.jspecify.annotations.Nullable;

import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Manages the visual representation of loading progress, displaying a background image,
 * a progress bar, and optional loading tips to the user.
 */
public final class ProgressForm {
    private static final int PROGRESSBAR_LOADINGTIP_SPACING = 45;
    private static final int NUM_TIPS = 39;
    private static final String TIP_PREFIX = "tip";
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ProgressForm.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static final String[] LOADING_TIPS = IntStream.range(0, NUM_TIPS)
            .mapToObj(idx -> i18n(TIP_PREFIX + idx))
            .toArray(String[]::new);

    /**
     * Visual display mode for progress screens.
     */
    public enum Mode {
        /** Initial game launch: Oddlabs logo, progress bar, no tips. */
        STARTUP,
        /** Loading an in-game match: Startup artwork, progress bar, loading tips. */
        GAME_LOAD,
        /** Loading a tutorial: Startup artwork, progress bar, no tips. */
        TUTORIAL,
        /** Returning to the main menu: Startup artwork, no progress bar, no tips. */
        MENU_RETURN
    }

    private static final long THROTTLE_INTERVAL_NANOS = 16_000_000L;

    private final NetworkSelector network;
    private final @Nullable ProgressBar progress_bar;
    private final ScreenManager screenManager;
    private final Runnable load_task;
    private long lastUpdateTime;
    private float currentProgress;

    /**
     * Visual layout parameters for a progress screen.
     */
    private record Layout(
                          String texture,
                          int textureWidth,
                          int textureHeight,
                          int imageWidth,
                          int imageHeight,
                          int progressX,
                          int progressY,
                          int progressWidth,
                          boolean showProgressBar,
                          boolean firstProgress
    ) {
    }

    public static void setProgressForm(NetworkSelector network, ScreenManager screenManager,
            @Nullable AudioManager audioManager,
            LoadCallback<GUIRoot, Screen> callback) {
        setProgressForm(network, screenManager, audioManager, callback, Mode.GAME_LOAD);
    }

    public static @Nullable Runnable setProgressForm(NetworkSelector network, final ScreenManager screenManager,
            @Nullable AudioManager audioManager,
            final LoadCallback<GUIRoot, Screen> callback, final Mode mode) {
        boolean show_tip = (mode == Mode.GAME_LOAD);
        Layout layout = switch (mode) {
            case STARTUP -> new Layout("/textures/gui/oddlabs", 1024, 1024, 800, 600, 320, 145, 200, true, true);
            case GAME_LOAD, TUTORIAL -> new Layout("/textures/gui/startup", 1024, 1024, 800, 600, 250, 145, 300, true,
                    false);
            case MENU_RETURN -> new Layout("/textures/gui/startup", 1024, 1024, 800, 600, 250, 145, 300, false, false);
        };

        ProgressForm form = new ProgressForm(network, screenManager, audioManager,
                callback, mode, layout, show_tip);

        return layout.firstProgress() ? form.getLoadTask() : null;
    }

    private ProgressForm(NetworkSelector network, final ScreenManager screenManager,
            @Nullable AudioManager audioManager,
            final LoadCallback<GUIRoot, Screen> callback,
            Mode mode, Layout layout, boolean show_tip) {
        this.network = network;
        this.screenManager = screenManager;
        this.load_task = () -> executeCallback(callback, audioManager);
        if (audioManager != null) {
            audioManager.stopSources();
        }
        var gui_root = (mode == Mode.STARTUP) ? screenManager.getGUIRoot() : screenManager.newFade(load_task);
        CameraDelegate<NullCamera> delegate = new NullDelegate(gui_root, false);
        gui_root.pushDelegate(delegate);

        int screen_width = gui_root.getWidth();
        int screen_height = gui_root.getHeight();
        int progress_width = (int) (layout.progressWidth() * (float) screen_width / layout.imageWidth());
        int progress_x = (int) (layout.progressX() * (float) screen_width / layout.imageWidth());
        int progress_y = (int) (layout.progressY() * (float) screen_height / layout.imageHeight());

        GUIImage image = new GUIImage(screen_width, screen_height, 0f, 0f, (float) layout.imageWidth() / layout
                .textureWidth(),
                (float) layout.imageHeight() / layout.textureHeight(), layout.texture());
        image.setPos(0, 0);
        delegate.addChild(image);

        if (layout.showProgressBar()) {
            ProgressBar bar = new ProgressBar(gui_root, progress_width, false);
            progress_y -= bar.getHeight();
            bar.setPos(progress_x, progress_y);
            delegate.addChild(bar);
            this.progress_bar = bar;
        } else {
            this.progress_bar = null;
        }

        if (show_tip && progress_bar != null) {
            var random = ThreadLocalRandom.current();
            CharSequence tip_string = LOADING_TIPS[random.nextInt(LOADING_TIPS.length)];
            var editFont = gui_root.getSkin().getEditFont();
            int tip_width = Math.min(gui_root.getWidth() - 10, editFont.getWidth(tip_string));
            LabelBox tip = new LabelBox(tip_string, editFont, tip_width);
            tip.setPos(progress_bar.getX() + progress_bar.getWidth() / 2 - tip.getWidth() / 2, progress_bar.getY() - tip
                    .getHeight() - PROGRESSBAR_LOADINGTIP_SPACING);
            delegate.addChild(tip);
        }

        // Force an initial render to show the progress screen immediately
        this.lastUpdateTime = System.nanoTime();
        screenManager.getEngine().updateProgress();
    }

    private Runnable getLoadTask() {
        return load_task;
    }

    private void executeCallback(LoadCallback<GUIRoot, Screen> callback,
            @Nullable AudioManager audioManager) {
        GUIRoot client_root = screenManager.createRoot();
        ProgressListener listener = new FormProgressListener();
        Screen screen = ProgressListener.supply(listener,
                () -> callback.load(client_root));
        if (progress_bar != null) {
            progress_bar.setProgress(1f);
        }
        screenManager.getEngine().updateProgress();
        screenManager.newFade(() -> {
            if (audioManager != null) {
                audioManager.startSources();
            }
        }, screen);
    }

    private final class FormProgressListener implements ProgressListener {
        @Override
        public void onProgress(float fraction) {
            currentProgress = Math.clamp(fraction, 0f, 1f);
            if (progress_bar != null) {
                progress_bar.setProgress(currentProgress);
            }
            network.tick();
            long now = System.nanoTime();
            if (now - lastUpdateTime >= THROTTLE_INTERVAL_NANOS) {
                lastUpdateTime = now;
                screenManager.getEngine().updateProgress();
            }
        }

        @Override
        public void onAdvance(float delta) {
            if (delta > 0f) {
                onProgress(currentProgress + delta);
            }
        }
    }
}
