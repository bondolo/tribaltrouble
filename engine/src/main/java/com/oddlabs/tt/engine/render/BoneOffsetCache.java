package com.oddlabs.tt.engine.render;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Allocation-free open-addressing cache for skeletal bone matrix offsets.
 */
final class BoneOffsetCache {
    private static final float DEFAULT_LOAD_FACTOR = 0.70f;

    private final @Nullable Object[] keys;
    private final int[] animations;
    private final float[] animTicks;
    private final int[] offsets;

    private final int mask;
    private final int threshold;
    private int size;

    BoneOffsetCache(int capacity) {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }

    BoneOffsetCache(int capacity, float loadFactor) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive: " + capacity);
        }
        if (loadFactor <= 0.0f || loadFactor >= 1.0f || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor must be between 0 and 1: " + loadFactor);
        }
        int minCapacity = (int) Math.ceil(capacity / loadFactor);
        int tableCapacity = 1;
        while (tableCapacity < minCapacity) {
            tableCapacity <<= 1;
        }
        tableCapacity = Math.max(tableCapacity, 4);

        this.keys = new Object[tableCapacity];
        this.animations = new int[tableCapacity];
        this.animTicks = new float[tableCapacity];
        this.offsets = new int[tableCapacity];
        this.mask = tableCapacity - 1;
        this.threshold = (int) (tableCapacity * loadFactor);
        this.size = 0;
    }

    /**
     * Retrieves the evaluated bone offset for the given skeletal pose, or -1 if not cached.
     *
     * @param key cache key identifying the skeletal model or sprite list
     * @param animation animation index
     * @param animTicks animation progress in ticks
     * @return non-negative bone offset, or -1 on cache miss
     */
    int get(Object key, int animation, float animTicks) {
        int index = hash(key, animation, animTicks) & mask;
        while (true) {
            @Nullable Object cachedKey = keys[index];
            if (cachedKey == null) {
                return -1;
            }
            if (cachedKey == key
                    && animations[index] == animation
                    && Float.floatToIntBits(this.animTicks[index]) == Float.floatToIntBits(animTicks)) {
                return offsets[index];
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * Caches the evaluated bone offset for the given skeletal pose.
     *
     * @param key cache key identifying the skeletal model or sprite list
     * @param animation animation index
     * @param animTicks animation progress in ticks
     * @param offset evaluated bone offset
     */
    void put(Object key, int animation, float animTicks, int offset) {
        int index = hash(key, animation, animTicks) & mask;
        while (true) {
            @Nullable Object cachedKey = keys[index];
            if (cachedKey == null) {
                if (size >= threshold) {
                    return;
                }
                keys[index] = key;
                animations[index] = animation;
                this.animTicks[index] = animTicks;
                offsets[index] = offset;
                size++;
                return;
            }
            if (cachedKey == key
                    && animations[index] == animation
                    && Float.floatToIntBits(this.animTicks[index]) == Float.floatToIntBits(animTicks)) {
                offsets[index] = offset;
                return;
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * Clears the cache for the start of a new frame.
     */
    void clear() {
        if (size > 0) {
            Arrays.fill(keys, null);
            size = 0;
        }
    }

    int size() {
        return size;
    }

    int threshold() {
        return threshold;
    }

    private int hash(Object key, int animation, float animTicks) {
        int h = System.identityHashCode(key);
        h = 31 * h + animation;
        h = 31 * h + Float.floatToIntBits(animTicks);
        return h ^ (h >>> 16);
    }
}
