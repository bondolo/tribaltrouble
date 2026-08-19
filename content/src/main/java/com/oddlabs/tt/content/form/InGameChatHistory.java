package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.net.ChatHistory;
import com.oddlabs.tt.net.ChatMessage;
import org.jspecify.annotations.NonNull;

/** History log for in-game chat messages. */
public final class InGameChatHistory extends ChatHistory {
    @Override
    public void chat(@NonNull ChatMessage message) {
        if (message.type() == ChatMessage.Type.PRIVATE ||
                message.type() == ChatMessage.Type.NORMAL ||
                message.type() == ChatMessage.Type.TEAM) {
            addMessage(message.formatLong());
        }
    }
}
