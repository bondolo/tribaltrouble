package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.base.util.ChatConsumer;
import com.oddlabs.tt.net.ChatHistory;
import com.oddlabs.tt.net.ChatMessage;

/** History log for in-game chat messages. */
public final class InGameChatHistory extends ChatHistory {
    @Override
    public void chat(ChatMessage message) {
        if (message.type() == ChatConsumer.Type.PRIVATE ||
                message.type() == ChatConsumer.Type.NORMAL ||
                message.type() == ChatConsumer.Type.TEAM) {
            addMessage(message.formatLong());
        }
    }
}
