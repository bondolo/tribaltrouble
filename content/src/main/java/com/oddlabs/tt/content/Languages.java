package com.oddlabs.tt.content;


import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.SequencedSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Supported languages and locale configuration for game content.
 */
public final class Languages {
    private static final SequencedSet<Locale> LANGUAGES;

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

    public static boolean hasLanguage(Locale locale) {
        return LANGUAGES.contains(locale) || LANGUAGES.contains(Locale.of(locale.getLanguage()));
    }

    public static SequencedSet<Locale> getLanguages() {
        return LANGUAGES;
    }
}
