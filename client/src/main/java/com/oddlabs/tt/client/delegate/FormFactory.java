package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.gui.Form;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

/**
 * Factory interface for lazily instantiating modal UI forms.
 *
 * @param <F> the concrete Form type
 */
@FunctionalInterface
public interface FormFactory<F extends Form> extends Supplier<F> {
    @NonNull
    F create();

    @Override
    default @NonNull F get() {
        return create();
    }
}
