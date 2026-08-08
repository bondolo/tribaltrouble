package com.oddlabs.tt.effects.particle;

public interface ParametricFunction {
    float getX(float u, float v);

    float getY(float u, float v);

    float getZ(float u, float v);
}
