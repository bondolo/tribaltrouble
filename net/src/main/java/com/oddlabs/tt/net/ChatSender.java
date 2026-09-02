package com.oddlabs.tt.net;

/** Transmitter for chat messages across a transport. */
@FunctionalInterface
public interface ChatSender {
    void sendChat(String text, ChatMessage.Type type);
}
