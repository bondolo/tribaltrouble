package com.oddlabs.tt.base.animation;


@FunctionalInterface
public interface Updatable<T> {
    void update(T anim);
}
