package com.oddlabs.tt.core.util;

import org.jspecify.annotations.NonNull;

/** Utility for sanitizing and formatting chat text. */
public final class SpamFilter {
    public static @NonNull String scan(String string) {
        string = string.replaceAll("\\s+", " ");
        string = string.replaceAll("\\.{3,}", "…");
        string = string.replaceAll("\\?+", "?");
        string = string.replaceAll("\\!+", "!");
        return string;
    }

    private SpamFilter() {
    }
}
