package com.oddlabs.tt.engine.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BoneOffsetCache}.
 */
final class BoneOffsetCacheTest {

    @Test
    void testPutAndGet() {
        BoneOffsetCache cache = new BoneOffsetCache(16);
        Object key = new Object();

        assertEquals(-1, cache.get(key, 0, 0.0f));

        cache.put(key, 0, 0.0f, 100);
        assertEquals(100, cache.get(key, 0, 0.0f));

        // Different animation
        assertEquals(-1, cache.get(key, 1, 0.0f));

        // Different animTicks
        assertEquals(-1, cache.get(key, 0, 0.5f));

        // Overwrite
        cache.put(key, 0, 0.0f, 200);
        assertEquals(200, cache.get(key, 0, 0.0f));
        assertEquals(1, cache.size());
    }

    @Test
    void testClear() {
        BoneOffsetCache cache = new BoneOffsetCache(16);
        Object key = new Object();

        cache.put(key, 0, 1.0f, 42);
        cache.put(key, 1, 2.0f, 84);
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertEquals(-1, cache.get(key, 0, 1.0f));
        assertEquals(-1, cache.get(key, 1, 2.0f));
    }

    @Test
    void testMultipleKeys() {
        BoneOffsetCache cache = new BoneOffsetCache(16);
        Object key1 = new Object();
        Object key2 = new Object();

        cache.put(key1, 0, 1.0f, 10);
        cache.put(key2, 0, 1.0f, 20);

        assertEquals(10, cache.get(key1, 0, 1.0f));
        assertEquals(20, cache.get(key2, 0, 1.0f));
    }

    @Test
    void testPredeterminedCapacityAndThreshold() {
        int requestedCapacity = 10;
        float loadFactor = 0.5f;
        BoneOffsetCache cache = new BoneOffsetCache(requestedCapacity, loadFactor);
        assertTrue(cache.threshold() >= requestedCapacity);

        Object key = new Object();
        int threshold = cache.threshold();
        for (int i = 0; i < threshold; i++) {
            cache.put(key, i, (float) i, i * 10);
        }
        assertEquals(threshold, cache.size());

        for (int i = 0; i < threshold; i++) {
            assertEquals(i * 10, cache.get(key, i, (float) i));
        }

        // Inserting beyond threshold should be safely ignored
        cache.put(key, 9999, 99.0f, 999);
        assertEquals(threshold, cache.size());
        assertEquals(-1, cache.get(key, 9999, 99.0f));
    }

    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new BoneOffsetCache(0));
        assertThrows(IllegalArgumentException.class, () -> new BoneOffsetCache(-5));
        assertThrows(IllegalArgumentException.class, () -> new BoneOffsetCache(16, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new BoneOffsetCache(16, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new BoneOffsetCache(16, Float.NaN));
    }
}
