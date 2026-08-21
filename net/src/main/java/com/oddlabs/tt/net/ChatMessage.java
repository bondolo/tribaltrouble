package com.oddlabs.tt.net;

import com.oddlabs.tt.base.util.SpamFilter;

/** Record representing a structured chat message payload. */
public record ChatMessage(String nick, String message, Type type) {
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
            case TEAM -> "(Team) " + formatShort();
            case PRIVATE -> "(Private) " + formatShort(); /* Fall through */
            case NORMAL, CHATROOM, GAME_MENU -> formatShort();
        };
    }
}
