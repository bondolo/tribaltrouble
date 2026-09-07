package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.KeyboardEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ActionControllerStackTest {

    private static final class TestController implements ActionController {
        private final String name;
        private final List<String> events;
        private boolean consumeInput;

        TestController(String name, List<String> events) {
            this.name = name;
            this.events = events;
        }

        @Override
        public boolean handleInput(InputEvent event) {
            events.add(name + ":handleInput");
            return consumeInput;
        }

        @Override
        public void onEnter() {
            events.add(name + ":onEnter");
        }

        @Override
        public void onExit() {
            events.add(name + ":onExit");
        }

        @Override
        public void onPause() {
            events.add(name + ":onPause");
        }

        @Override
        public void onResume() {
            events.add(name + ":onResume");
        }
    }

    @Test
    void testPushAndPopLifecycle() {
        ActionControllerStack stack = new ActionControllerStack();
        List<String> events = new ArrayList<>();
        List<@Nullable ActionController> notified = new ArrayList<>();
        stack.addListener(notified::add);

        TestController root = new TestController("root", events);
        TestController sub = new TestController("sub", events);

        stack.push(root);
        assertSame(root, stack.getActiveController());
        assertEquals(1, stack.size());
        assertEquals(List.of("root:onEnter"), events);
        assertEquals(List.of(root), notified);

        stack.push(sub);
        assertSame(sub, stack.getActiveController());
        assertEquals(2, stack.size());
        assertEquals(List.of("root:onEnter", "root:onPause", "sub:onEnter"), events);
        assertEquals(List.of(root, sub), notified);

        ActionController popped = stack.pop();
        assertSame(sub, popped);
        assertSame(root, stack.getActiveController());
        assertEquals(1, stack.size());
        assertEquals(List.of("root:onEnter", "root:onPause", "sub:onEnter", "sub:onExit", "root:onResume"), events);
        assertEquals(List.of(root, sub, root), notified);

        ActionController poppedRoot = stack.pop();
        assertSame(root, poppedRoot);
        assertNull(stack.getActiveController());
        assertTrue(stack.isEmpty());
        assertEquals(List.of("root:onEnter", "root:onPause", "sub:onEnter", "sub:onExit", "root:onResume",
                "root:onExit"), events);
        assertEquals(java.util.Arrays.asList(root, sub, root, null), notified);

        assertNull(stack.pop());
    }

    @Test
    void testSetRootAndClear() {
        ActionControllerStack stack = new ActionControllerStack();
        List<String> events = new ArrayList<>();

        TestController c1 = new TestController("c1", events);
        TestController c2 = new TestController("c2", events);
        TestController c3 = new TestController("c3", events);

        stack.push(c1);
        stack.push(c2);
        events.clear();

        stack.setRoot(c3);
        assertSame(c3, stack.getActiveController());
        assertEquals(1, stack.size());
        assertEquals(List.of("c2:onExit", "c1:onExit", "c3:onEnter"), events);

        events.clear();
        stack.clear();
        assertNull(stack.getActiveController());
        assertTrue(stack.isEmpty());
        assertEquals(List.of("c3:onExit"), events);
    }

    @Test
    void testHandleInputDelegation() {
        ActionControllerStack stack = new ActionControllerStack();
        List<String> events = new ArrayList<>();
        TestController c1 = new TestController("c1", events);
        KeyboardEvent kbEvent = new KeyboardEvent(Key.A, 'a', false, false, false, false, 1);
        InputEvent inputEvent = new InputEvent(kbEvent, Set.of(), InputPhase.PRESSED);

        assertFalse(stack.handleInput(inputEvent));

        stack.push(c1);
        assertFalse(stack.handleInput(inputEvent));
        assertEquals(List.of("c1:onEnter", "c1:handleInput"), events);

        c1.consumeInput = true;
        assertTrue(stack.handleInput(inputEvent));
    }

    @Test
    void testWithActive() {
        ActionControllerStack stack = new ActionControllerStack();
        List<String> events = new ArrayList<>();
        TestController c1 = new TestController("c1", events);

        boolean[] ran = new boolean[1];
        stack.withActive(TestController.class, _ -> ran[0] = true);
        assertFalse(ran[0]);

        stack.push(c1);
        stack.withActive(TestController.class, _ -> ran[0] = true);
        assertTrue(ran[0]);
    }

    @Test
    void testBind() {
        ActionControllerStack stack = new ActionControllerStack();
        List<String> events = new ArrayList<>();
        TestController c1 = new TestController("c1", events);

        boolean[] ran = new boolean[1];
        Runnable bound = stack.bind(TestController.class, _ -> ran[0] = true);
        bound.run();
        assertFalse(ran[0]);

        stack.push(c1);
        bound.run();
        assertTrue(ran[0]);
    }
}
