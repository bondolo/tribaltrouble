package com.oddlabs.matchservlet;

/**
 * Input validation helpers for registration and account details.
 */
public final class Validation {
    public static final int MAX_EMAIL_LENGTH = 60;

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= MAX_EMAIL_LENGTH && email.matches(".+@.+\\..+");
    }
}
