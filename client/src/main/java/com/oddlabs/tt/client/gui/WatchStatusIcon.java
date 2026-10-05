package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.ReproduceUnitContainer;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.util.Color;

import com.oddlabs.tt.engine.font.Font;

/**
 * Status icon displaying training and reproduction countdown metrics.
 */
public final class WatchStatusIcon extends StatusIcon {
    private static final Color.Linear COLOR = Color.Linear.WHITE.alpha(0.75f);
    private final GUIIcons icons;
    private Building building;

    public WatchStatusIcon(Font font, int label_width, IconQuad icon, String tooltip, GUIIcons icons) {
        super(font, label_width, icon, tooltip);
        this.icons = icons;
    }

    public void setUnitContainerBuilding(Building building) {
        this.building = building;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        super.renderGeometry(renderer);
        if (!building.isDead() && !building.getChieftainContainer().orElseThrow().isTraining() && building.getOwner()
                .getUnitCountContainer().getNumSupplies() < building.getOwner().getWorld().getMaxUnitCount()) {
            float progress = ((ReproduceUnitContainer) (building.getUnitContainer().orElseThrow())).getBuildProgress();
            var watch = icons.getWatch(progress);
            int x = getWidth() - watch.getWidth();
            int y = (getHeight() - watch.getHeight()) / 2;
            x -= 5; // visual HAX
            renderer.drawTexture(watch.getTexture(), x, y, watch.getWidth(), watch.getHeight(),
                    watch.getU1(), watch.getV1(), watch.getU2(), watch.getV2(), COLOR);
        }
    }
}
