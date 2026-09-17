package com.oddlabs.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link TickTimeManager}.
 */
class TickTimeManagerTest {

    @Test
    void testDefaultsAndAdvance() {
        TickTimeManager manager = new TickTimeManager();
        assertEquals(0L, manager.getMillis());
        assertEquals(0L, manager.getTick());
        assertEquals(20L, manager.getMillisPerTick());

        assertEquals(20L, manager.advance());
        assertEquals(20L, manager.getMillis());
        assertEquals(1L, manager.getTick());

        assertEquals(60L, manager.advance(40L));
        assertEquals(60L, manager.getMillis());
        assertEquals(3L, manager.getTick());
    }

    @Test
    void testCustomConfiguration() {
        TickTimeManager manager = new TickTimeManager(100L, 10L);
        assertEquals(100L, manager.getMillis());
        assertEquals(10L, manager.getTick());
        assertEquals(10L, manager.getMillisPerTick());

        assertEquals(110L, manager.advance());
        assertEquals(110L, manager.getMillis());
        assertEquals(11L, manager.getTick());
    }

    @Test
    void testInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TickTimeManager(-1L, 20L));
        assertThrows(IllegalArgumentException.class, () -> new TickTimeManager(0L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new TickTimeManager(0L, -5L));

        TickTimeManager manager = new TickTimeManager();
        assertThrows(IllegalArgumentException.class, () -> manager.advance(-10L));
    }
}
