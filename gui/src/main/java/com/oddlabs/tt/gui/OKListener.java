package com.oddlabs.tt.gui;

import com.oddlabs.tt.gui.event.MouseClickListener;

public final class OKListener implements MouseClickListener {
    private final Form form;

    public OKListener(Form form) {
        this.form = form;
    }

    @Override
    public void mouseClicked(MouseButton button, int x, int y, int clicks) {
        form.remove();
    }
}
