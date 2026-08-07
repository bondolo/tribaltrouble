package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.SequencedSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Supported languages and flag icon mappings.
 */
public final class Languages {
    private static final SequencedSet<@NonNull Locale> LANGUAGES;

    static {
        var langs = new TreeSet<>(Comparator.comparing(Locale::toLanguageTag));
        langs.addAll(Set.of(
                Locale.forLanguageTag("da"),
                Locale.forLanguageTag("de"),
                Locale.forLanguageTag("en"),
                Locale.forLanguageTag("es"),
                Locale.forLanguageTag("it"),
                Locale.forLanguageTag("pt-BR")));
        LANGUAGES = Collections.unmodifiableSequencedSet(langs);
    }

    private Languages() {
    }

    public static boolean hasLanguage(@NonNull Locale locale) {
        return LANGUAGES.contains(locale) || LANGUAGES.contains(Locale.of(locale.getLanguage()));
    }

    public static @NonNull SequencedSet<@NonNull Locale> getLanguages() {
        return LANGUAGES;
    }
}
