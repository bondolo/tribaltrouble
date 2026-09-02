package com.oddlabs.tt.net;

import com.oddlabs.tt.base.util.SpamFilter;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

/** Record representing a structured chat message payload. */
public record ChatMessage(String nick, String message, Type type) {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ChatMessage.class.getName());

    public enum Type {
        NORMAL,
        TEAM,
        PRIVATE,
        CHATROOM,
        GAME_MENU
    }

    public ChatMessage(String nick, String message, Type type) {
        this.nick = nick;
        this.message = SpamFilter.scan(message);
        this.type = type;
    }

    public String formatShort() {
        return "<" + nick + "> " + message;
    }

    public String formatLong() {
        return switch (type) {
            case TEAM -> Utils.getBundleString(bundle, "team_prefix", formatShort());
            case PRIVATE -> Utils.getBundleString(bundle, "private_prefix", formatShort());
            case NORMAL, CHATROOM, GAME_MENU -> formatShort();
        };
    }
}
