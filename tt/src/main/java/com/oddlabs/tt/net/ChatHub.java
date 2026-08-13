package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/** Event dispatcher broadcasting chat messages to registered listeners. */
public final class ChatHub implements ChatListener {
    private final CopyOnWriteArraySet<@NonNull ChatListener> listeners = new CopyOnWriteArraySet<>();
    private @Nullable Predicate<String> ignore_filter;

    public void setIgnoreFilter(@Nullable Predicate<String> ignore_filter) {
        this.ignore_filter = ignore_filter;
    }

    public boolean addListener(@NonNull ChatListener listener) {
        return listeners.add(listener);
    }

    public boolean removeListener(@NonNull ChatListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void chat(@NonNull ChatMessage message) {
        if (ignore_filter == null || !ignore_filter.test(message.nick())) {
            listeners.forEach(listener -> listener.chat(message));
        }
    }
}
