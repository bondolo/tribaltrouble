package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.base.resource.NativeResource;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.gui.render.TextLineRenderer;
import com.oddlabs.util.Color;

import java.util.function.DoubleSupplier;

/**
 * Diagnostics HUD overlay rendering system memory usage and frame rate.
 */
public final class Status {
    private final Settings settings;
    private final DoubleSupplier fpsSupplier;
    private final StringBuilder buf = new StringBuilder();

    public Status(Settings settings, DoubleSupplier fpsSupplier) {
        this.settings = settings;
        this.fpsSupplier = fpsSupplier;
    }

    public void render(GUIRenderer renderer) {
        long free_mem = Runtime.getRuntime().freeMemory();
        buf.delete(0, buf.length());
        if (settings.inDeveloperMode()) {
            buf.append("JHeap ")
                    .append(free_mem)
                    .append("(");
            int total_jheap = (int) (Runtime.getRuntime().totalMemory() / (1024 * 1024));
            buf.append(total_jheap)
                    .append("M) globj ")
                    .append(NativeResource.getCount());
        }
        double fps = fpsSupplier.getAsDouble();
        long fpsDisplay = fps > 0 ? Math.round(1000.0 / fps) : 0;
        buf.append(" FPS ")
                .append(fpsDisplay)
                .append(" (")
                .append(Math.round(fps))
                .append(" ms/frame)");

        TextLineRenderer.render(renderer, Skin.getSkin().getEditFont(), buf, 0, 0, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY, Color.Standard.WHITE);
    }
}
