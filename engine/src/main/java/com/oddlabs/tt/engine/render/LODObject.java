package com.oddlabs.tt.engine.render;


import org.jspecify.annotations.NonNull;

public interface LODObject {
    void markDetailPoint();

    void markDetailPolygon(@NonNull PolyDetail level);

    int getTriangleCount(@NonNull PolyDetail level);

    float getEyeDistanceSquared();
}
