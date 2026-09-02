package com.oddlabs.tt.content.skirmish;

import com.oddlabs.tt.content.menu.Menu;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;

/**
 * Form wrapper embedding the skirmish terrain configuration menu.
 */
public final class TerrainMenuForm extends Form implements TerrainMenuListener {
    private final TerrainMenu terrain;

    public TerrainMenuForm(Menu main_menu) {
        terrain = new TerrainMenu(main_menu.getGUIRoot(), main_menu, false, this);
        addChild(terrain);
        terrain.place();
        compileCanvas();
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            terrain.getButtonOK().setFocus(direction);
        }
    }

    @Override
    public void terrainMenuCancel() {
        cancel();
    }

    @Override
    public void terrainMenuOK() {

    }
}
