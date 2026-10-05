package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.CancelListener;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.QuestionForm;
import com.oddlabs.tt.gui.event.MouseClickListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.viewer.WorldViewer;

import java.util.ResourceBundle;

/**
 * Modal dialog displayed while waiting for network players to connect or synchronize.
 */
public final class WaitingForPlayersForm extends Form {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(WaitingForPlayersForm.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final WorldViewer viewer;

    public WaitingForPlayersForm(WorldViewer viewer) {
        super(viewer.getGUIRoot(), "");
        this.viewer = viewer;
        var info_label = new Label(i18n("waiting"), skin.getHeadlineFont());
        info_label.setDim(280, info_label.getHeight());
        HorizButton abort_button = new HorizButton(viewer.getGUIRoot(), i18n("abort"), 120);
        abort_button.addMouseClickListener(new AbortListener());
        addChild(info_label);
        addChild(abort_button);
        info_label.place();
        abort_button.place(Origin.AT_END);
        compileCanvas();
        centerPos();
    }

    @Override
    protected void handleInput(InputEvent event) {
        if (event.consumeAction(GameAction.UI_CANCEL)) {
            // KEY_ESCAPE should not close this form
            // Swallow escape.
            return;
        }
        super.handleInput(event);
    }

    @Override
    protected void doCancel() {
        viewer.close();
    }

    private final class AbortListener implements MouseClickListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            viewer.getGUIRoot().addModalForm(new QuestionForm(viewer.getGUIRoot(), i18n("confirm_abort"),
                    new CancelListener(
                            WaitingForPlayersForm.this)));
        }
    }
}
