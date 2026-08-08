package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.client.gui.Form;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

@FunctionalInterface
interface FormFactory<F extends Form> extends Supplier<F> {
    @NonNull
    F create();

    @Override
    default @NonNull F get() {
        return create();
    }
}
