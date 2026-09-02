package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.delegate.ControllableCameraDelegate;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.RadioButton;
import com.oddlabs.tt.gui.RadioButtonGroup;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.TextBox;
import com.oddlabs.tt.gui.event.EnterListener;
import com.oddlabs.tt.net.ChatHistory;
import com.oddlabs.tt.net.ChatHub;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.ChatSender;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_LEFT;

/** In-game chat overlay UI form. */
public final class InGameChatForm extends Form implements ChatListener {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(InGameChatForm.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static final int CHAT_WIDTH = 400;
    private static final int BUTTON_WIDTH = 50;
    private static final int CHAT_HEIGHT = 150;

    private final EditLine chat_line;
    private final TextBox chat_box;
    private final RadioButtonGroup radio_button_group;
    private final RadioButton radio_all;
    private final RadioButton radio_team;
    private final ChatHub chatHub;
    private final ChatHistory history;
    private final ChatSender chatSender;

    public InGameChatForm(ChatHub chatHub, ChatHistory history, ChatSender chatSender) {
        super(i18n("chat"));
        this.chatHub = chatHub;
        this.history = history;
        this.chatSender = chatSender;

        chat_line = new EditLine(CHAT_WIDTH, 256);
        addChild(chat_line);
        chat_line.addEnterListener(new ChatListener());

        HorizButton button_send = new HorizButton(i18n("send"), BUTTON_WIDTH);
        addChild(button_send);
        button_send.addMouseClickListener((_, _, _, _) -> chat_line.enterPressedAll());

        chat_box = new TextBox(CHAT_WIDTH + BUTTON_WIDTH, CHAT_HEIGHT, Skin.getSkin().getEditFont(), Integer.MAX_VALUE);
        addChild(chat_box);

        radio_button_group = new RadioButtonGroup();

        radio_all = new RadioButton(true, radio_button_group, i18n("send_to_all"));
        addChild(radio_all);

        radio_team = new RadioButton(false, radio_button_group, i18n("send_to_team"));
        addChild(radio_team);

        chat_line.place();
        button_send.place(chat_line, RIGHT_MID);
        chat_box.place(chat_line, TOP_LEFT);
        radio_all.place(chat_line, BOTTOM_LEFT);
        radio_team.place(chat_line, BOTTOM_RIGHT);
        compileCanvas();
        history.clear();
    }

    @Override
    protected void doAdd() {
        super.doAdd();
        chatHub.addListener(this);
        refreshMessages();
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        chatHub.removeListener(this);
    }

    public void setReceivers(boolean all) {
        radio_button_group.mark(all ? radio_all : radio_team);
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            chat_line.setFocus(direction);
        }
    }

    @Override
    public void chat(ChatMessage message) {
        refreshMessages();
    }

    private void refreshMessages() {
        var messages = history.getMessages();
        chat_box.setText(String.join("\n", messages));
        chat_box.setOffsetY(Integer.MAX_VALUE);
    }

    @Override
    public void mouseMoved(int x, int y) {
        ((ControllableCameraDelegate<?>) getParent()).mouseMoved(x, y);
    }

    private final class ChatListener implements EnterListener {
        @Override
        public void enterPressed(CharSequence text) {
            if (!text.isEmpty()) {
                var chat = text.toString();
                chat_line.clear();
                ChatMessage.Type type = (radio_button_group.getMarked() == radio_team)
                        ? ChatMessage.Type.TEAM
                        : ChatMessage.Type.NORMAL;
                chatSender.sendChat(chat, type);
            } else {
                cancel();
            }
        }
    }
}
