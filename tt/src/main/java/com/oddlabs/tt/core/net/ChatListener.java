package com.oddlabs.tt.core.net;

@FunctionalInterface
public interface ChatListener {
    void chat(ChatMessage message);
}
