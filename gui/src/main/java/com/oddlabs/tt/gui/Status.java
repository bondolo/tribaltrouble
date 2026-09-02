package com.oddlabs.tt.gui;

import com.oddlabs.tt.gui.render.TextLineRenderer;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.engine.settings.Settings;
import com.oddlabs.tt.base.resource.NativeResource;
import com.oddlabs.util.Color;

public final class Status {
    private final Settings settings;
    private final StringBuilder buf = new StringBuilder();

    public Status(Settings settings) {
        this.settings = settings;
    }

    public void render(GUIRenderer renderer) {
        long free_mem = Runtime.getRuntime().freeMemory();
        buf.delete(0, buf.length());
        if (settings.inDeveloperMode()) {
            buf.append("TPF ")
                    .append(Renderer.getTrianglesRendered())
                    .append(" JHeap ")
                    .append(free_mem)
                    .append("(");
            int total_jheap = (int) (Runtime.getRuntime().totalMemory() / (1024 * 1024));
            buf.append(total_jheap)
                    .append("M) globj ")
                    .append(NativeResource.getCount());
            /*			float x = gui_root.getLandscapeLocationX();
            			float y = gui_root.getLandscapeLocationY();
            			if (UnitGrid.getGrid() != null) {
            				int grid_x = UnitGrid.getGrid().toGridCoordinate(x);
            				int grid_y = UnitGrid.getGrid().toGridCoordinate(y);
            				buf.append(" X ");
            				    .append(grid_x,);
            				    .append(" Y ")
            				    .append(grid_y);
            			}*/
        }
        buf.append(" FPS ")
                .append(Math.round(1000f / Renderer.getFPS()))
                .append(" (")
                .append(Math.round(Renderer.getFPS()))
                .append(" ms/frame)");

        TextLineRenderer.render(renderer, Skin.getSkin().getEditFont(), buf, 0, 0, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY, Color.Standard.WHITE);
    }
}
