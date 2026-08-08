package com.oddlabs.tt.core.animation;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface Updatable<T> {
    void update(@NonNull T anim);
}
