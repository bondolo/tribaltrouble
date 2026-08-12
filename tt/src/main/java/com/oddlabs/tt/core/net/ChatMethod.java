package com.oddlabs.tt.core.net;

import com.oddlabs.tt.core.util.InfoPrinter;

@FunctionalInterface
public interface ChatMethod {
    void execute(InfoPrinter info_printer, MatchmakingClient client, String text);
}
