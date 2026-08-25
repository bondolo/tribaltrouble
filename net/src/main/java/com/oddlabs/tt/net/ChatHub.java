package com.oddlabs.tt.net;

import com.oddlabs.tt.base.util.ChatConsumer;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/** Event dispatcher broadcasting chat messages to registered listeners. */
public final class ChatHub implements ChatListener {
    private final CopyOnWriteArraySet<ChatListener> listeners = new CopyOnWriteArraySet<>();
    private final Map<ChatConsumer, ChatListener> consumerMap = new ConcurrentHashMap<>();
    private @Nullable Predicate<String> ignore_filter;

    public void setIgnoreFilter(@Nullable Predicate<String> ignore_filter) {
        this.ignore_filter = ignore_filter;
    }

    public boolean addListener(ChatListener listener) {
        return listeners.add(listener);
    }

    public boolean removeListener(ChatListener listener) {
        return listeners.remove(listener);
    }

    public boolean addConsumer(ChatConsumer consumer) {
        ChatListener listener = msg -> consumer.chat(msg.formatShort(), msg.type());
        consumerMap.put(consumer, listener);
        return addListener(listener);
    }

    public boolean removeConsumer(ChatConsumer consumer) {
        ChatListener listener = consumerMap.remove(consumer);
        return listener != null && removeListener(listener);
    }

    @Override
    public void chat(ChatMessage message) {
        if (ignore_filter == null || !ignore_filter.test(message.nick())) {
            listeners.forEach(listener -> listener.chat(message));
        }
    }
}
