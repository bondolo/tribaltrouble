package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.base.util.InfoPrinter;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.ChatMethod;
import com.oddlabs.tt.net.ChatSender;
import com.oddlabs.tt.net.MatchmakingClient;
import com.oddlabs.tt.net.PeerHub;

import java.util.Map;

/**
 * Chat sender for in-game sessions that routes slash commands to registered handlers and delegates
 * peer chat to the peer hub.
 */
public final class InGameChatSender implements ChatSender {
    private final InfoPrinter infoPrinter;
    private final MatchmakingClient matchmakingClient;
    private final Map<String, ChatMethod> commands;
    private final PeerHub peerHub;

    public InGameChatSender(InfoPrinter infoPrinter, MatchmakingClient matchmakingClient,
            Map<String, ChatMethod> commands, PeerHub peerHub) {
        this.infoPrinter = infoPrinter;
        this.matchmakingClient = matchmakingClient;
        this.commands = Map.copyOf(commands);
        this.peerHub = peerHub;
    }

    @Override
    public void sendChat(String text, ChatMessage.Type type) {
        if (!ChatCommand.filterCommand(infoPrinter, matchmakingClient, commands, text)) {
            String trimmed = text.trim();
            ChatMethod directMethod = commands.get(trimmed.toLowerCase());
            if (directMethod != null) {
                directMethod.execute(infoPrinter, matchmakingClient, "");
                return;
            }
            peerHub.sendChat(text, type);
        }
    }
}
