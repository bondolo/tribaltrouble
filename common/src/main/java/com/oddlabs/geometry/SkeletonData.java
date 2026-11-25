package com.oddlabs.geometry;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Metadata describing bone names and initial poses for a sprite skeleton.
public record SkeletonData(String[] boneNames, Map<String, float[]> initialPoses) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// Finds the index of a bone by exact name or space-delimited suffix match (e.g., `"Prop1"` matching `"warrior
  /// Prop1"`), returning `-1` if not found.
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

  public boolean hasBone(String name) {
    return findBoneIndex(name) >= 0;
  }

  /// Returns the 16-element initial pose matrix for `boneIndex`, or `null` if the index is out of range or has no pose.
  public float @Nullable [] getInitialPose(int boneIndex) {
    if (boneIndex < 0 || boneIndex >= boneNames.length) {
      return null;
    }
    String name = boneNames[boneIndex];
    return initialPoses.get(name);
  }
}
