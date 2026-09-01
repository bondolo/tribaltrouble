package com.oddlabs.tt.simulation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link DistributableTable}.
 */
class DistributableTableTest {

    private static final class TestDistributable implements Distributable {
    }

    @Test
    void testRegisterAndLookup() {
        DistributableTable table = new DistributableTable();
        TestDistributable d1 = new TestDistributable();
        TestDistributable d2 = new TestDistributable();

        int id1 = table.register(d1);
        int id2 = table.register(d2);

        assertEquals(1, id1);
        assertEquals(2, id2);

        assertEquals(id1, table.getName(d1));
        assertEquals(id2, table.getName(d2));

        assertEquals(d1, table.getDistributable(id1));
        assertEquals(d2, table.getDistributable(id2));
    }

    @Test
    void testUnregister() {
        DistributableTable table = new DistributableTable();
        TestDistributable d1 = new TestDistributable();

        int id1 = table.register(d1);
        assertEquals(d1, table.getDistributable(id1));

        table.unregister(d1);
        assertNull(table.getDistributable(id1));
    }

    @Test
    void testUnregisteredGetDistributableReturnsNull() {
        DistributableTable table = new DistributableTable();
        assertNull(table.getDistributable(999));
    }
}
