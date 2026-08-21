package com.oddlabs.tt.gui;

import com.oddlabs.tt.gui.event.MouseClickListener;

public final class CancelListener implements MouseClickListener {
    private final Form form;

    public CancelListener(Form form) {
        this.form = form;
    }

    @Override
    public void mouseClicked(MouseButton button, int x, int y, int clicks) {
        form.cancel();
    }
}
