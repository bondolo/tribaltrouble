package com.oddlabs.tt.engine.render;

/** GPU work regions measured with timer queries, in submission order. */
public enum GpuPass {
    LANDSCAPE,
    SHADOWS,
    UNITS,
    TREES,
    PLANTS,
    WATER,
    SKY,
    EFFECTS,
    COMPOSITE,
    GUI
}
