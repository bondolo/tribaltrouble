package com.oddlabs.tt.client.gui;

import com.oddlabs.matchmaking.ChatRoomUser;
import com.oddlabs.tt.core.net.ChatHistory;
import com.oddlabs.tt.core.net.ChatMessage;
import com.oddlabs.tt.core.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * History of messages and user events in a chat room.
 */
public final class ChatRoomHistory extends ChatHistory {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ChatRoomHistory.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private @Nullable ChatRoomUser[] old_users;

    public void update(ChatRoomUser @NonNull [] new_users) {
        if (old_users == null) {
            old_users = Arrays.copyOf(new_users, new_users.length);
            return;
        }
        Set<ChatRoomUser> new_users_set = new HashSet<>(Arrays.asList(new_users));
        Set<ChatRoomUser> old_users_set = new HashSet<>(Arrays.asList(old_users));
        Set<ChatRoomUser> joined_users = new HashSet<>(new_users_set);
        joined_users.removeAll(old_users_set);
        for (ChatRoomUser user : joined_users) {
            addMessage(i18n("user_joined", user.getNick()));
        }
        Set<ChatRoomUser> left_users = new HashSet<>(old_users_set);
        left_users.removeAll(new_users_set);
        for (ChatRoomUser user : left_users) {
            addMessage(i18n("user_left", user.getNick()));
        }
        old_users = Arrays.copyOf(new_users, new_users.length);
    }

    @Override
    public void chat(@NonNull ChatMessage message) {
        if (message.type() != ChatMessage.Type.PRIVATE && message.type() != ChatMessage.Type.CHATROOM)
            return;
        addMessage(message.formatLong());
    }
}
