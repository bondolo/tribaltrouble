package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.simulation.model.Unit;

public interface MagicFactory {
    float getHitRadius();

    float getSecondsPerAnim();

    float getSecondsPerInit();

    float getSecondsPerRelease();

    Magic execute(Unit src);
}
