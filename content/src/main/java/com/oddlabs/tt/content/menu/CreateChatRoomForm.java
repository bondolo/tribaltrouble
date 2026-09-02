package com.oddlabs.tt.content.menu;

import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.gui.ButtonObject;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.event.EnterListener;
import com.oddlabs.tt.gui.event.MouseClickListener;
import com.oddlabs.tt.net.MatchmakingClient;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;

public final class CreateChatRoomForm extends Form {
    private static final int BUTTON_WIDTH = 100;
    private static final int EDITLINE_WIDTH = 200;

    private static final ResourceBundle bundle = ResourceBundle.getBundle(CreateChatRoomForm.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final MatchmakingClient client;
    private final EditLine editline_room_name;

    public CreateChatRoomForm(SelectGameMenu menu) {
        this.client = menu.getMainMenu().getEngine().getNetwork().getMatchmakingClient();

        // headline
        Label label_headline = new Label(i18n("caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        // name
        CreateListener create_listener = new CreateListener();
        Label name_label = new Label(i18n("room_name"), Skin.getSkin().getEditFont());
        editline_room_name = new EditLine(EDITLINE_WIDTH, 200);
        editline_room_name.addEnterListener(create_listener);
        addChild(name_label);
        addChild(editline_room_name);

        // buttons
        ButtonObject button_ok = new OKButton(BUTTON_WIDTH);
        button_ok.addMouseClickListener(create_listener);
        addChild(button_ok);
        ButtonObject button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> this.cancel());
        addChild(button_cancel);

        // place
        label_headline.place();
        editline_room_name.place(label_headline, BOTTOM_LEFT);
        name_label.place(editline_room_name, LEFT_MID);

        button_cancel.place(Origin.AT_END);
        button_ok.place(button_cancel, LEFT_MID);

        compileCanvas();
        centerPos();
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            editline_room_name.setFocus(direction);
        }
    }

    private void create() {
        String name = editline_room_name.getContents().trim();
        if (name.length() >= MatchmakingServerInterface.MIN_ROOM_NAME_LENGTH) {
            remove();
            client.joinRoom(name);
        }
    }

    private final class CreateListener implements MouseClickListener, EnterListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            create();
        }

        @Override
        public void enterPressed(CharSequence text) {
            create();
        }
    }
}
