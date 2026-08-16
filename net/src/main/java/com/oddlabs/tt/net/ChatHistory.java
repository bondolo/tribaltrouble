package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.SequencedCollection;

/**
 * Base class for managing a rolling history of chat messages.
 */
public abstract class ChatHistory implements ChatListener {
    private static final int MAX_HISTORY = 50;

    private final SequencedCollection<@NonNull String> messages = new ArrayDeque<>(MAX_HISTORY);

    public final void clear() {
        messages.clear();
    }

    @Override
    public abstract void chat(ChatMessage message);

    protected final void addMessage(String msg) {
        while (messages.size() >= MAX_HISTORY) {
            messages.removeFirst();
        }
        messages.add(msg);
    }

    public final @NonNull SequencedCollection<@NonNull String> getMessages() {
        return messages;
    }
}
