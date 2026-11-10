package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.NonFocusIconButton;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.simulation.model.ChieftainContainer;
import org.jspecify.annotations.Nullable;

/** Non-focusable icon button that trains a chieftain. */
public final class ChieftainButton extends NonFocusIconButton {
    private final GUIIcons icons;
    private @Nullable Building current_building;

    public ChieftainButton(WorldViewer viewer, ModeIconQuads icon, GUIIcons icons) {
        super(viewer.getGUIRoot(), icon, GameAction.TRAIN_CHIEFTAIN, () -> ActionButtonPanel.i18n(
                "train_chieftain_tip", viewer.getInputManager().getBindingString(
                        GameAction.TRAIN_CHIEFTAIN)));
        this.icons = icons;
        setCanFocus(true);
    }

    public final void setBuilding(Building current_building) {
        this.current_building = current_building;
    }

    @Override
    protected void postRender(GUIRenderer renderer) {
        if (current_building != null && current_building.isAlive() && current_building.getChieftainContainer()
                .map(ChieftainContainer::isTraining).orElse(false)) {
            var watchQuad = icons.getWatch(getProgress());
            renderer.drawIcon(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight());
        }
    }

    private float getProgress() {
        return current_building != null && current_building.isAlive() ? current_building.getChieftainContainer()
                .map(ChieftainContainer::getBuildProgress).orElse(0f) : 0;
    }
}
