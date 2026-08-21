package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.IconButton;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.GUIRenderer;

public class MapIslandButton extends IconButton {
    private final int islandIndex;

    public MapIslandButton(ModeIconQuads icon, int islandIndex) {
        super(icon, () -> "");
        this.islandIndex = islandIndex;
        setCanFocus(true);
    }

    public int getIslandIndex() {
        return islandIndex;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode;
        if (isDisabled()) {
            skinMode = ModeIconQuads.Mode.DISABLED;
        } else if (isHovered()) {
            skinMode = ModeIconQuads.Mode.ACTIVE;
        } else if (isActive()) {
            boolean otherIslandHovered = false;
            GUIRoot root = getParentGUIRoot();
            if (root != null) {
                GUIObject hoverObj = root.getCurrentGUIObject();
                if (hoverObj instanceof MapIslandButton && hoverObj != this) {
                    otherIslandHovered = true;
                }
            }
            skinMode = otherIslandHovered ? ModeIconQuads.Mode.NORMAL : ModeIconQuads.Mode.ACTIVE;
        } else {
            skinMode = ModeIconQuads.Mode.NORMAL;
        }
        renderer.drawModeIcon(getIcon(), skinMode, 0, 0);
    }
}
