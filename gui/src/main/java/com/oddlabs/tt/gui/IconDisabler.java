package com.oddlabs.tt.gui;

/** Predicate callback for dynamically disabling GUI action buttons. */
@FunctionalInterface
public interface IconDisabler {
    boolean isDisabled();
}
