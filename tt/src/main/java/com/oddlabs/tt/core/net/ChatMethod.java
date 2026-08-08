package com.oddlabs.tt.core.net;

import com.oddlabs.tt.client.gui.InfoPrinter;

@FunctionalInterface
public interface ChatMethod {
    void execute(InfoPrinter info_printer, String text);
}
