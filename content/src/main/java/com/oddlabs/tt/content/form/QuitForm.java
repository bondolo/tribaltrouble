package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

/** Confirmation dialog for quitting matches. */
public final class QuitForm extends QuestionForm {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(QuitForm.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public QuitForm(final GUIRoot gui_root) {
        super(i18n(PeerHub.isWaitingForAck() ? "confirm_quit_waiting_for_ack" : "confirm_quit"),
                (_, _, _, _) -> Renderer.shutdown());
    }

    @Override
    public void handleInput(InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED) {
            if (event.consumeAction(GameAction.GLOBAL_QUIT)) {
                Renderer.shutdown();
                event.consume();
                return;
            }
        }
        super.handleInput(event);
    }
}
