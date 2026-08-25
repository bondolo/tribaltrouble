package com.oddlabs.tt.base.util;

/**
 * Consumer for formatted chat messages.
 */
public interface ChatConsumer extends InfoPrinter {
    enum Type {
        NORMAL,
        TEAM,
        PRIVATE,
        CHATROOM,
        GAME_MENU
    }

    void chat(String message, Type type);
}
