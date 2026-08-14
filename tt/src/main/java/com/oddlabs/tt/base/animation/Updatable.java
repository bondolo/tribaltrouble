package com.oddlabs.tt.base.animation;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface Updatable<T> {
    void update(@NonNull T anim);
}
