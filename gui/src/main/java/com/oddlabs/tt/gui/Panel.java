package com.oddlabs.tt.gui;


public class Panel extends Group {
    private final PanelTab tab;

    public Panel(CharSequence caption) {
        super(true); // Ensure Panel is focusable
        tab = new PanelTab(caption);
    }

    public final PanelTab getTab() {
        return tab;
    }

    @Override
    public final void compileCanvas() {
        Box box = Skin.getSkin().getPanelData().box();
        super.compileCanvas(box.getLeftOffset(), box.getBottomOffset(), box.getRightOffset(), box.getTopOffset());
    }
}
