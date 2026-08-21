package com.oddlabs.tt.engine.render;


public interface LODObject {
    void markDetailPoint();

    void markDetailPolygon(PolyDetail level);

    int getTriangleCount(PolyDetail level);

    float getEyeDistanceSquared();
}
