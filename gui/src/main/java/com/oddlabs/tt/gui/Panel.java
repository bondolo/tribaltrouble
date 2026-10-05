package com.oddlabs.tt.gui;


/**
 * Container card representing a single selectable tab view in a panel group.
 */
public class Panel extends Group {
    private final PanelTab tab;

    public Panel(GUIRoot guiRoot, CharSequence caption) {
        super(guiRoot, true); // Ensure Panel is focusable
        this.tab = new PanelTab(guiRoot, caption);
    }

    public final PanelTab getTab() {
        return tab;
    }

    @Override
    public final void compileCanvas() {
        Box box = getSkin().getPanelData().box();
        super.compileCanvas(box.getLeftOffset(), box.getBottomOffset(), box.getRightOffset(), box.getTopOffset());
    }
}
