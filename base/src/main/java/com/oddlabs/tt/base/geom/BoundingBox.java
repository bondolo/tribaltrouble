package com.oddlabs.tt.base.geom;

import com.oddlabs.tt.base.global.AppConfig;
import org.joml.FrustumIntersection;
import org.joml.Intersectionf;
import org.joml.Matrix4fc;
import org.joml.Vector2f;

/**
 * Provides world-space bounding volumes for collision detection, frustum culling, and spatial sorting.
 */
public class BoundingBox {

    public float bmin_x = Float.POSITIVE_INFINITY;
    public float bmin_y = Float.POSITIVE_INFINITY;
    public float bmin_z = Float.POSITIVE_INFINITY;
    public float bmax_x = Float.NEGATIVE_INFINITY;
    public float bmax_y = Float.NEGATIVE_INFINITY;
    public float bmax_z = Float.NEGATIVE_INFINITY;

    public BoundingBox() {
    }

    public BoundingBox(BoundingBox other) {
        setBounds(other);
    }

    public BoundingBox(float bmin_x, float bmax_x, float bmin_y, float bmax_y, float bmin_z, float bmax_z) {
        setBounds(bmin_x, bmax_x, bmin_y, bmax_y, bmin_z, bmax_z);
    }

    @Override
    public String toString() {
        return "bmx " + bmin_x + " bmy " + bmin_y + " bmz " + bmin_z + " bxx " + bmax_x + " bxy " + bmax_y + " bxz "
                + bmax_z;
    }

    public final boolean isValid() {
        return bmin_x <= bmax_x && bmin_y <= bmax_y && bmin_z <= bmax_z;
    }

    public final boolean contains(BoundingBox other) {
        if (AppConfig.LEGACY_DETERMINISTIC) {
            // Legacy simulation containment used an asymmetric strict inequality on minimum planes
            // (other.bmin > this.bmin), excluding boundary-coincident models. Preserved for bit-exact
            // legacy deterministic replay parity.
            return other.bmax_x <= this.bmax_x && other.bmin_x > this.bmin_x
                    && other.bmax_y <= this.bmax_y && other.bmin_y > this.bmin_y
                    && other.bmax_z <= this.bmax_z && other.bmin_z > this.bmin_z;
        }
        // Modern containment uses inclusive checks on all planes to ensure reflexivity (a box contains
        // itself), align with standard AABB conventions and ElementNode.doInsertElement partition
        // logic, and avoid spurious tree reinsertions for boundary models.
        return other.bmax_x <= this.bmax_x && other.bmin_x >= this.bmin_x
                && other.bmax_y <= this.bmax_y && other.bmin_y >= this.bmin_y
                && other.bmax_z <= this.bmax_z && other.bmin_z >= this.bmin_z;
    }

    public final float getCX() {
        return (bmax_x + bmin_x) * 0.5f;
    }

    public final float getCY() {
        return (bmax_y + bmin_y) * 0.5f;
    }

    public final float getCZ() {
        return (bmax_z + bmin_z) * 0.5f;
    }

    public final void expandBoundsX(float x) {
        if (x < bmin_x) {
            bmin_x = x;
        }
        if (x > bmax_x) {
            bmax_x = x;
        }
    }

    public final void expandBoundsY(float y) {
        if (y < bmin_y) {
            bmin_y = y;
        }
        if (y > bmax_y) {
            bmax_y = y;
        }
    }

    public final void expandBoundsZ(float z) {
        if (z < bmin_z) {
            bmin_z = z;
        }
        if (z > bmax_z) {
            bmax_z = z;
        }
    }

    public final void expandBounds(float bmin_x, float bmax_x, float bmin_y, float bmax_y, float bmin_z, float bmax_z) {
        if (bmin_x < this.bmin_x) this.bmin_x = bmin_x;
        if (bmax_x > this.bmax_x) this.bmax_x = bmax_x;
        if (bmin_y < this.bmin_y) this.bmin_y = bmin_y;
        if (bmax_y > this.bmax_y) this.bmax_y = bmax_y;
        if (bmin_z < this.bmin_z) this.bmin_z = bmin_z;
        if (bmax_z > this.bmax_z) this.bmax_z = bmax_z;
    }

    public final void expandBoundsXY(BoundingBox other) {
        if (other.bmin_x < bmin_x) bmin_x = other.bmin_x;
        if (other.bmax_x > bmax_x) bmax_x = other.bmax_x;
        if (other.bmin_y < bmin_y) bmin_y = other.bmin_y;
        if (other.bmax_y > bmax_y) bmax_y = other.bmax_y;
    }

    public void expandBounds(BoundingBox other) {
        if (other.bmin_x < bmin_x) bmin_x = other.bmin_x;
        if (other.bmax_x > bmax_x) bmax_x = other.bmax_x;
        if (other.bmin_y < bmin_y) bmin_y = other.bmin_y;
        if (other.bmax_y > bmax_y) bmax_y = other.bmax_y;
        if (other.bmin_z < bmin_z) bmin_z = other.bmin_z;
        if (other.bmax_z > bmax_z) bmax_z = other.bmax_z;
    }

    public final void resetBounds() {
        this.bmin_x = Float.POSITIVE_INFINITY;
        this.bmin_y = Float.POSITIVE_INFINITY;
        this.bmin_z = Float.POSITIVE_INFINITY;
        this.bmax_x = Float.NEGATIVE_INFINITY;
        this.bmax_y = Float.NEGATIVE_INFINITY;
        this.bmax_z = Float.NEGATIVE_INFINITY;
    }

    public final void setBounds(BoundingBox other) {
        this.bmin_x = other.bmin_x;
        this.bmax_x = other.bmax_x;
        this.bmin_y = other.bmin_y;
        this.bmax_y = other.bmax_y;
        this.bmin_z = other.bmin_z;
        this.bmax_z = other.bmax_z;
    }

    public final void setBounds(float bmin_x, float bmax_x, float bmin_y, float bmax_y, float bmin_z, float bmax_z) {
        this.bmin_x = bmin_x;
        this.bmax_x = bmax_x;
        this.bmin_y = bmin_y;
        this.bmax_y = bmax_y;
        this.bmin_z = bmin_z;
        this.bmax_z = bmax_z;
    }

    /**
     * Sets this bounding box to the minimal axis-aligned bounding box enclosing
     * the source bounding box transformed by an affine matrix.
     *
     * @param source the source bounding box to transform
     * @param matrix the affine transformation matrix
     */
    public final void setTransformedBounds(BoundingBox source, Matrix4fc matrix) {
        float minX = matrix.m30();
        float maxX = minX;
        float m00 = matrix.m00();
        if (m00 > 0f) {
            minX += m00 * source.bmin_x;
            maxX += m00 * source.bmax_x;
        } else {
            minX += m00 * source.bmax_x;
            maxX += m00 * source.bmin_x;
        }
        float m10 = matrix.m10();
        if (m10 > 0f) {
            minX += m10 * source.bmin_y;
            maxX += m10 * source.bmax_y;
        } else {
            minX += m10 * source.bmax_y;
            maxX += m10 * source.bmin_y;
        }
        float m20 = matrix.m20();
        if (m20 > 0f) {
            minX += m20 * source.bmin_z;
            maxX += m20 * source.bmax_z;
        } else {
            minX += m20 * source.bmax_z;
            maxX += m20 * source.bmin_z;
        }

        float minY = matrix.m31();
        float maxY = minY;
        float m01 = matrix.m01();
        if (m01 > 0f) {
            minY += m01 * source.bmin_x;
            maxY += m01 * source.bmax_x;
        } else {
            minY += m01 * source.bmax_x;
            maxY += m01 * source.bmin_x;
        }
        float m11 = matrix.m11();
        if (m11 > 0f) {
            minY += m11 * source.bmin_y;
            maxY += m11 * source.bmax_y;
        } else {
            minY += m11 * source.bmax_y;
            maxY += m11 * source.bmin_y;
        }
        float m21 = matrix.m21();
        if (m21 > 0f) {
            minY += m21 * source.bmin_z;
            maxY += m21 * source.bmax_z;
        } else {
            minY += m21 * source.bmax_z;
            maxY += m21 * source.bmin_z;
        }

        float minZ = matrix.m32();
        float maxZ = minZ;
        float m02 = matrix.m02();
        if (m02 > 0f) {
            minZ += m02 * source.bmin_x;
            maxZ += m02 * source.bmax_x;
        } else {
            minZ += m02 * source.bmax_x;
            maxZ += m02 * source.bmin_x;
        }
        float m12 = matrix.m12();
        if (m12 > 0f) {
            minZ += m12 * source.bmin_y;
            maxZ += m12 * source.bmax_y;
        } else {
            minZ += m12 * source.bmax_y;
            maxZ += m12 * source.bmin_y;
        }
        float m22 = matrix.m22();
        if (m22 > 0f) {
            minZ += m22 * source.bmin_z;
            maxZ += m22 * source.bmax_z;
        } else {
            minZ += m22 * source.bmax_z;
            maxZ += m22 * source.bmin_z;
        }

        setBounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    private float computeXYRadius() {
        float longest_x = Math.max(bmax_x, -bmin_x);
        float longest_y = Math.max(bmax_y, -bmin_y);
        return (float) Math.sqrt(longest_x * longest_x + longest_y * longest_y);
    }

    /**
     * Make the bounding box contain all rotation of the current box, in the XY plane
     */
    public final void maximizeXYPlane() {
        float len = computeXYRadius();

        bmin_x = -len;
        bmax_x = len;
        bmin_y = -len;
        bmax_y = len;
    }

    /**
     * Tests whether this bounding box is partly or completely within the frustum.
     *
     * @param frustum the frustum intersection culler
     * @return {@code true} if inside or intersecting; {@code false} if completely outside
     */
    public final boolean testFrustum(FrustumIntersection frustum) {
        return frustum.testAab(bmin_x, bmin_y, bmin_z, bmax_x, bmax_y, bmax_z);
    }

    /**
     * Tests this bounding box against the frustum.
     *
     * @param frustum the frustum intersection culler
     * @return index of the culling plane (&gt;= 0) if outside;
     *         {@link FrustumIntersection#INSIDE} (-2) if completely inside;
     *         or {@link FrustumIntersection#INTERSECT} (-1) if intersecting.
     */
    public final int intersectFrustum(FrustumIntersection frustum) {
        return frustum.intersectAab(bmin_x, bmin_y, bmin_z, bmax_x, bmax_y, bmax_z);
    }

    /**
     * Tests whether the given ray intersects this bounding box and stores the near and far
     * ray parameter values in {@code result}.
     *
     * @param originX ray origin X
     * @param originY ray origin Y
     * @param originZ ray origin Z
     * @param dirX ray direction X
     * @param dirY ray direction Y
     * @param dirZ ray direction Z
     * @param result vector receiving {@code (tNear, tFar)} on intersection
     * @return {@code true} if the ray intersects this bounding box
     */
    public final boolean intersectsRay(float originX, float originY, float originZ,
            float dirX, float dirY, float dirZ, Vector2f result) {
        return Intersectionf.intersectRayAab(
                originX, originY, originZ,
                dirX, dirY, dirZ,
                bmin_x, bmin_y, bmin_z,
                bmax_x, bmax_y, bmax_z,
                result);
    }
}
