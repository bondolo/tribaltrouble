package com.oddlabs.geometry;

import java.io.Serial;
import java.io.Serializable;

/**
 * Axis-aligned bounding box extents for geometry models and animations.
 *
 * @param minX minimum X coordinate
 * @param maxX maximum X coordinate
 * @param minY minimum Y coordinate
 * @param maxY maximum Y coordinate
 * @param minZ minimum Z coordinate
 * @param maxZ maximum Z coordinate
 */
public record BoundsData(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) implements
        Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
