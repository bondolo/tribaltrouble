package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.client.render.*;
import com.oddlabs.tt.effects.render.*;

import org.jspecify.annotations.NonNull;

public interface LODObject {
    void markDetailPoint();

    void markDetailPolygon(@NonNull PolyDetail level);

    int getTriangleCount(@NonNull PolyDetail level);

    float getEyeDistanceSquared();
}
