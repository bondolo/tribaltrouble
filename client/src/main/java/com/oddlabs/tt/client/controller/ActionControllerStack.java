package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.input.InputEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages a stack of action controllers, routing input events to the active top controller
 * and firing lifecycle and state change notifications.
 */
public final class ActionControllerStack {
    private final Deque<ActionController> stack = new ArrayDeque<>();
    private final List<Consumer<@Nullable ActionController>> listeners = new ArrayList<>();

    public void push(ActionController controller) {
        ActionController current = stack.peek();
        if (current != null) {
            current.onPause();
        }
        stack.push(controller);
        controller.onEnter();
        notifyListeners(controller);
    }

    public @Nullable ActionController pop() {
        if (stack.isEmpty()) {
            return null;
        }
        ActionController popped = stack.pop();
        popped.onExit();
        ActionController next = stack.peek();
        if (next != null) {
            next.onResume();
        }
        notifyListeners(next);
        return popped;
    }

    public void setRoot(@Nullable ActionController controller) {
        while (!stack.isEmpty()) {
            stack.pop().onExit();
        }
        if (controller != null) {
            stack.push(controller);
            controller.onEnter();
        }
        notifyListeners(controller);
    }

    public void clear() {
        setRoot(null);
    }

    public @Nullable ActionController getActiveController() {
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public boolean handleInput(InputEvent event) {
        ActionController active = stack.peek();
        return active != null && active.handleInput(event);
    }

    public <T extends ActionController> void withActive(Class<T> type, Consumer<T> action) {
        ActionController active = stack.peek();
        if (type.isInstance(active)) {
            action.accept(type.cast(active));
        }
    }

    public <T extends ActionController> Runnable bind(Class<T> type, Consumer<T> action) {
        return () -> withActive(type, action);
    }

    public void addListener(Consumer<@Nullable ActionController> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<@Nullable ActionController> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(@Nullable ActionController activeController) {
        for (Consumer<@Nullable ActionController> listener : List.copyOf(listeners)) {
            listener.accept(activeController);
        }
    }
}
