package com.oddlabs.tt.client.form;

import com.oddlabs.tt.client.gui.CancelListener;
import com.oddlabs.tt.client.gui.Form;
import com.oddlabs.tt.client.gui.HorizButton;
import com.oddlabs.tt.client.gui.Label;
import com.oddlabs.tt.client.gui.MouseButton;
import com.oddlabs.tt.client.gui.Origin;
import com.oddlabs.tt.client.gui.Skin;
import com.oddlabs.tt.client.guievent.MouseClickListener;
import com.oddlabs.tt.client.input.GameAction;
import com.oddlabs.tt.client.input.InputEvent;
import com.oddlabs.tt.core.util.Utils;
import com.oddlabs.tt.client.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class WaitingForPlayersForm extends Form {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(WaitingForPlayersForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final WorldViewer viewer;

    public WaitingForPlayersForm(WorldViewer viewer) {
        this.viewer = viewer;
        var info_label = new Label(i18n("waiting"), Skin.getSkin().getHeadlineFont());
        info_label.setDim(280, info_label.getHeight());
        HorizButton abort_button = new HorizButton(i18n("abort"), 120);
        abort_button.addMouseClickListener(new AbortListener());
        addChild(info_label);
        addChild(abort_button);
        info_label.place();
        abort_button.place(Origin.AT_END);
        compileCanvas();
        centerPos();
    }

    @Override
    protected void handleInput(@NonNull InputEvent event) {
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
        public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
            viewer.getGUIRoot().addModalForm(new QuestionForm(i18n("confirm_abort"), new CancelListener(
                    WaitingForPlayersForm.this)));
        }
    }
}
