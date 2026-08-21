package com.oddlabs.tt.client.delegate;

import com.oddlabs.tt.gui.Form;

import java.util.function.Supplier;

/**
 * Factory interface for lazily instantiating modal UI forms.
 *
 * @param <F> the concrete Form type
 */
@FunctionalInterface
public interface FormFactory<F extends Form> extends Supplier<F> {
    F create();

    @Override
    default F get() {
        return create();
    }
}
