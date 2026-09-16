package com.oddlabs.tt.engine.resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a {@link TextureGenerator} provider by a unique symbolic name for asset referencing.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NamedGenerator {
    /**
     * The unique name used to look up this texture generator.
     */
    String value();
}
