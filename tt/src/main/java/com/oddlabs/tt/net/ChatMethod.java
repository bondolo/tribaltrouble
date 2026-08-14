package com.oddlabs.tt.net;

import com.oddlabs.tt.base.util.InfoPrinter;

@FunctionalInterface
public interface ChatMethod {
    void execute(InfoPrinter info_printer, MatchmakingClient client, String text);
}
