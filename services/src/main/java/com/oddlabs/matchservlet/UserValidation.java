package com.oddlabs.matchservlet;

import org.jspecify.annotations.Nullable;

/**
 * Validates usernames and email addresses against matchmaking constraints.
 */
final class UserValidation {
    static final int MAX_EMAIL_LENGTH = 60;

    private UserValidation() {
    }

    static boolean checkChars(String value, String allowedChars) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (allowedChars.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }

    static boolean isValidEmail(@Nullable String email) {
        return email != null && email.length() <= MAX_EMAIL_LENGTH && email.matches(".+@.+\\..+");
    }
}
