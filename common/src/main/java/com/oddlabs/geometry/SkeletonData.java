package com.oddlabs.geometry;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Metadata describing bone names and initial poses for a sprite skeleton.
 */
public record SkeletonData(
                           String[] boneNames,
                           Map<String, float[]> initialPoses
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Finds the index of a bone by exact name, or by suffix match (e.g. "Prop1" matching "warrior Prop1").
     *
     * @param name the bone or socket name to find
     * @return the bone index, or -1 if not found
     */
    public int findBoneIndex(String name) {
        for (int i = 0; i < boneNames.length; i++) {
            if (boneNames[i].equals(name)) {
                return i;
            }
        }
        for (int i = 0; i < boneNames.length; i++) {
            String b = boneNames[i];
            if (b.endsWith(name) && (b.length() == name.length() || b.charAt(b.length() - name.length() - 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if a bone exists by exact or suffix match.
     *
     * @param name the bone or socket name
     * @return true if the bone exists
     */
    public boolean hasBone(String name) {
        return findBoneIndex(name) >= 0;
    }

    /**
     * Retrieves the 16-element initial pose matrix for the bone at the given index.
     *
     * @param boneIndex the bone index
     * @return the initial pose float array, or null if invalid index or pose not found
     */
    public float @Nullable [] getInitialPose(int boneIndex) {
        if (boneIndex < 0 || boneIndex >= boneNames.length) {
            return null;
        }
        String name = boneNames[boneIndex];
        return initialPoses.get(name);
    }
}
