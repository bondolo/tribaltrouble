package com.oddlabs.tt.model;

import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public sealed interface Accessory permits AbstractAccessory {
    /** Unit with which the accessory is associated */
    @NonNull Unit getUnit();
    @NonNull SpriteKey getSpriteRenderer();
}
