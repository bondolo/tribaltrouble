package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.matchmaking.ChatRoomUser;
import com.oddlabs.tt.client.viewer.ChatCommand;
import com.oddlabs.tt.gui.event.EnterListener;
import com.oddlabs.tt.gui.event.ItemChosenListener;
import com.oddlabs.tt.gui.event.MouseClickListener;
import com.oddlabs.tt.gui.event.RowListener;
import com.oddlabs.tt.engine.ClientEngine;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.ChatRoomInfo;
import com.oddlabs.tt.base.util.Utils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_TOP;

/** UI component displaying live chat room messages and user lists. */
public class ChatPanel extends Panel implements ChatListener {
    private static final int PULLDOWN_INDEX_MESSAGE = 0;
    private static final int PULLDOWN_INDEX_INFO = 1;
    private static final int PULLDOWN_INDEX_IGNORE = 2;

    private final MultiColumnComboBox<ChatRoomUser> lobby_users_list_box;
    private final MultiColumnComboBox<ChatRoomUser> playing_users_list_box;
    private final TextBox chat_box;
    private final EditLine chat_line;
    private final GUIRoot gui_root;
    private final ClientEngine engine;

    private final int user_list_width;

    private @Nullable PrivateMessageForm private_message_form;

    private static ResourceBundle getBundle() {
        return ResourceBundle.getBundle(ChatPanel.class.getName());
    }

    private static String getI18N(String key) {
        return Utils.getBundleString(getBundle(), key);
    }

    public ChatPanel(GUIRoot gui_root, ClientEngine engine, ChatRoomInfo info, int compare_width, int compare_height,
            int button_width, EnterListener chat_listener, MouseClickListener leave_listener) {
        super(getI18N("chat"));
        this.gui_root = gui_root;
        this.engine = engine;
        FormData fdata = Skin.getSkin().getFormData();
        Box pdata = Skin.getSkin().getPanelData().box();
        Box edata = Skin.getSkin().getEditBox();

        Label label_headline = new Label(info.name(), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        int edit_line_height = edata.getBottomOffset() + edata.getTopOffset() + Skin.getSkin().getEditFont()
                .getHeight();
        int height = compare_height - pdata.getTopOffset() - pdata.getBottomOffset() - edit_line_height - label_headline
                .getHeight() - 2 * fdata.objectSpacing();
        int user_list_height = (height - Skin.getSkin().getFormData().objectSpacing()) / 2;//- Skin.getSkin().editFont().getHeight();
        user_list_width = 2 * button_width + 2 * fdata.objectSpacing() - Skin.getSkin().getScrollBarData().scrollBar()
                .getWidth();

        ColumnInfo[] lobby_infos = new ColumnInfo[]{
                new ColumnInfo(getI18N("lobby"), user_list_width)};
        lobby_users_list_box = new MultiColumnComboBox<>(gui_root, lobby_infos, user_list_height, true);

        ColumnInfo[] playing_infos = new ColumnInfo[]{
                new ColumnInfo(getI18N("playing"), user_list_width)};
        playing_users_list_box = new MultiColumnComboBox<>(gui_root, playing_infos, user_list_height, true);

        PulldownMenu<ChatRoomUser> lobby_pulldown_menu = new PulldownMenu<>();
        lobby_pulldown_menu.addItem(new PulldownItem<>(getI18N("message")));
        lobby_pulldown_menu.addItem(new PulldownItem<>(getI18N("info")));
        lobby_pulldown_menu.addItem(new PulldownItem<>(""));
        lobby_pulldown_menu.addItemChosenListener(new PulldownListener(lobby_users_list_box));
        lobby_users_list_box.setPulldownMenu(lobby_pulldown_menu);

        ChatRoomUserDoubleClickedListener lobby_double_clicked = new ChatRoomUserDoubleClickedListener(
                lobby_pulldown_menu);
        lobby_users_list_box.addRowListener(lobby_double_clicked);
        addChild(lobby_users_list_box);

        PulldownMenu<ChatRoomUser> playing_pulldown_menu = new PulldownMenu<>();
        playing_pulldown_menu.addItem(new PulldownItem<>(getI18N("message")));
        playing_pulldown_menu.addItem(new PulldownItem<>(getI18N("info")));
        playing_pulldown_menu.addItem(new PulldownItem<>(""));
        playing_pulldown_menu.addItemChosenListener(new PulldownListener(playing_users_list_box));
        playing_users_list_box.setPulldownMenu(playing_pulldown_menu);

        ChatRoomUserDoubleClickedListener playing_double_clicked = new ChatRoomUserDoubleClickedListener(
                playing_pulldown_menu);
        playing_users_list_box.addRowListener(playing_double_clicked);
        addChild(playing_users_list_box);

        chat_box = new TextBox(compare_width - user_list_width - fdata.objectSpacing() - Skin.getSkin()
                .getScrollBarData().scrollBar().getWidth(), height, Skin.getSkin().getEditFont(), Integer.MAX_VALUE);
        addChild(chat_box);

        chat_line = new EditLine(compare_width - button_width - fdata.objectSpacing(), 256);
        chat_line.addEnterListener(chat_listener);
        addChild(chat_line);

        HorizButton button_send = new HorizButton(getI18N("send"), button_width);
        button_send.addMouseClickListener((_, _, _, _) -> chat_line.enterPressedAll());
        addChild(button_send);

        HorizButton button_leave = new HorizButton(getI18N("leave"), button_width);
        button_leave.addMouseClickListener(leave_listener);
        addChild(button_leave);

        // Place objects
        label_headline.place();
        chat_box.place(label_headline, BOTTOM_LEFT);
        lobby_users_list_box.place(chat_box, RIGHT_TOP);
        playing_users_list_box.place(lobby_users_list_box, BOTTOM_LEFT);
        chat_line.place(chat_box, BOTTOM_LEFT);
        button_send.place(chat_line, RIGHT_MID);
        button_leave.place(playing_users_list_box, BOTTOM_LEFT);
        compileCanvas();

        update(info);
    }

    public final void update(ChatRoomInfo info) {
        lobby_users_list_box.clear();
        playing_users_list_box.clear();
        for (ChatRoomUser user : info.users()) {
            Row<ChatRoomUser, Label> row = new Row<>(List.of(
                    new Label(user.getNick(), Skin.getSkin().getMultiColumnComboBoxData().font(), user_list_width)),
                    user);
            if (user.isPlaying()) {
                playing_users_list_box.addRow(row);
            } else {
                lobby_users_list_box.addRow(row);
            }
        }
    }

    @Override
    public void chat(ChatMessage message) {
        if (message.type() != ChatMessage.Type.PRIVATE && message.type() != ChatMessage.Type.CHATROOM)
            return;
        if (message.type() != ChatMessage.Type.PRIVATE) {
            getTab().updateNotify();
        }
        refreshMessages();
    }

    private void refreshMessages() {
        var messages = engine.getNetwork().getMatchmakingClient().getChatRoomHistory();
        chat_box.setText(String.join("\n", messages));
        chat_box.setOffsetY(Integer.MAX_VALUE);
    }

    @Override
    public final void setFocus() {
        chat_line.setFocus();
    }

    public final void connectionLost() {
        if (private_message_form != null)
            private_message_form.remove();
    }

    private final class PulldownListener implements ItemChosenListener<ChatRoomUser> {
        private final MultiColumnComboBox<ChatRoomUser> box;

        PulldownListener(MultiColumnComboBox<ChatRoomUser> box) {
            this.box = box;
        }

        @Override
        public void itemChosen(PulldownMenu<ChatRoomUser> menu, int item_index) {
            ChatRoomUser user = box.getRightClickedRowData();
            String nick = user.getNick();
            switch (item_index) {
                case PULLDOWN_INDEX_MESSAGE -> gui_root.addModalForm(new PrivateMessageForm(engine.getNetwork()
                        .getMatchmakingClient(), nick));
                case PULLDOWN_INDEX_INFO -> engine.getNetwork().getMatchmakingClient().requestInfo(nick);
                case PULLDOWN_INDEX_IGNORE -> {
                    if (ChatCommand.isIgnoring(nick))
                        ChatCommand.unignore(gui_root.getInfoPrinter(), nick);
                    else
                        ChatCommand.ignore(gui_root.getInfoPrinter(), nick);
                }
                default -> throw new IllegalArgumentException("Unexpected pulldown index");
            }
            box.setFocus();
        }
    }

    private final class ChatRoomUserDoubleClickedListener implements RowListener<ChatRoomUser> {
        private final PulldownMenu<ChatRoomUser> menu;

        ChatRoomUserDoubleClickedListener(PulldownMenu<ChatRoomUser> menu) {
            this.menu = menu;
        }

        @Override
        public void rowDoubleClicked(ChatRoomUser user) {
            private_message_form = new PrivateMessageForm(engine.getNetwork().getMatchmakingClient(), user.getNick());
            gui_root.addModalForm(private_message_form);
        }

        @Override
        public void rowChosen(ChatRoomUser user) {
            String item_text = ChatCommand.isIgnoring(user.getNick())
                    ? getI18N("unignore") : getI18N("ignore");
            menu.getItem(PULLDOWN_INDEX_IGNORE).ifPresent(pi -> pi.setLabelString(item_text));
        }
    }

}
