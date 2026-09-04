package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.net.MatchmakingClient;
import com.oddlabs.tt.gui.ButtonObject;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Origin.AT_END;
import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * Modal form for sending a private message to a specific user.
 */
public final class PrivateMessageForm extends Form {
    private static final int BUTTON_WIDTH = 100;
    private static final int EDITLINE_WIDTH = 240;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(PrivateMessageForm.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final EditLine editline_name;
    private final String nick;
    private final MatchmakingClient client;

    public PrivateMessageForm(MatchmakingClient client, String nick) {
        this.client = client;
        this.nick = nick;
        // headline
        Label label_headline = new Label(i18n("private_message_caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        Label label_name = new Label(i18n("to", nick), Skin.getSkin().getEditFont());
        editline_name = new EditLine(EDITLINE_WIDTH, 256);
        editline_name.addEnterListener(_ -> send());

        addChild(label_name);
        addChild(editline_name);


        ButtonObject button_ok = new OKButton(BUTTON_WIDTH);
        button_ok.addMouseClickListener((MouseButton _, int _, int _, int _) -> send());
        ButtonObject button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> this.cancel());

        addChild(button_ok);
        addChild(button_cancel);

        // Place objects
        label_headline.place();
        label_name.place(label_headline, BOTTOM_LEFT);
        editline_name.place(label_name, RIGHT_MID);
        button_cancel.place(AT_END);
        button_ok.place(button_cancel, LEFT_MID);
        compileCanvas();
        centerPos();
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            editline_name.setFocus(direction);
        }
    }

    private void send() {
        String message = editline_name.getContents();
        client.sendPrivateMessage(nick, message);
        remove();
    }
}
