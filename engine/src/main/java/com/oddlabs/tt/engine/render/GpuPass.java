package com.oddlabs.tt.engine.render;

/** GPU work regions measured with timer queries, in submission order. */
public enum GpuPass {
    SKY,
    LANDSCAPE,
    SHADOWS,
    UNITS,
    TREES,
    PLANTS,
    WATER,
    EFFECTS,
    COMPOSITE,
    GUI
}
