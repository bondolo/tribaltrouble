package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.input.InputEvent;

/**
 * Contextual controller for player actions and input handling.
 */
public interface ActionController {
    boolean handleInput(InputEvent event);

    default void onEnter() {
    }

    default void onExit() {
    }

    default void onPause() {
    }

    default void onResume() {
    }
}
