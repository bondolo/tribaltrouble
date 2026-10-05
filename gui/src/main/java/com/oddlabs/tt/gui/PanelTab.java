package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.util.Color;

import java.util.Objects;

/**
 * Interactive tab header button representing a panel within a panel group.
 */
public final class PanelTab extends GUIObject {
    private static final Color.Linear HIGHLIGHT_COLOR = Color.Linear.GREEN;
    private final Skin skin;
    private final PanelData panelData;
    private boolean selected;
    private final Label label;

    PanelTab(GUIRoot guiRoot, CharSequence caption) {
        this.skin = Objects.requireNonNull(guiRoot.getSkin(), "Skin cannot be null");
        this.panelData = skin.getPanelData();
        Font font = skin.getButtonFont();
        label = new Label(caption, font);
        label.setPos(panelData.leftCaptionOffset(), (panelData.tab().getHeight() - font.getHeight()) / 2 + panelData
                .bottomCaptionOffset());
        addChild(label);
        setDim(panelData.leftCaptionOffset() + label.getWidth() + panelData.rightCaptionOffset(), panelData.tab()
                .getHeight());
        setCanFocus(true);
        setTabStop(false); // control-tab is used for cycling panels, they aren't in the standard tab order.
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    public final void select(boolean selected) {
        this.selected = selected;
        focusNotifyAll(false);
        if (selected)
            label.setColor(Label.DEFAULT_COLOR);
    }

    public final ModeIconQuads.Mode getRenderState() {
        return isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive() || selected
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;
    }

    @Override
    protected final void renderGeometry(GUIRenderer renderer) {
        panelData.tab()
                .render(renderer, 0, 0, getWidth(), getRenderState());
    }

    public final void updateNotify() {
        if (!selected)
            label.setColor(HIGHLIGHT_COLOR);
    }
}
