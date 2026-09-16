package com.oddlabs.tt.engine.resource;

import com.oddlabs.tt.engine.render.Texture;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Base class and service provider interface for procedural texture generators.
 */
public abstract class TextureGenerator implements Supplier<Texture[]> {
    private static final Map<String, Supplier<TextureGenerator>> NAMED_GENERATORS = new ConcurrentHashMap<>();
    private static volatile boolean providersLoaded = false;

    private static void ensureProvidersLoaded() {
        if (!providersLoaded) {
            synchronized (NAMED_GENERATORS) {
                if (!providersLoaded) {
                    loadProviders(ServiceLoader.load(TextureGenerator.class, TextureGenerator.class.getClassLoader()));
                    if (NAMED_GENERATORS.isEmpty()) {
                        loadProviders(ServiceLoader.load(TextureGenerator.class));
                    }
                    providersLoaded = true;
                }
            }
        }
    }

    private static void loadProviders(ServiceLoader<TextureGenerator> loader) {
        for (ServiceLoader.Provider<TextureGenerator> provider : loader.stream().toList()) {
            NamedGenerator named = provider.type().getAnnotation(NamedGenerator.class);
            if (named != null) {
                NAMED_GENERATORS.put(named.value(), provider);
            }
        }
    }

    /**
     * Looks up a registered {@link TextureGenerator} by its {@link NamedGenerator} name.
     *
     * @param name the generator name
     * @return a new instance of the texture generator, or null if not found
     */
    public static @Nullable TextureGenerator findNamed(String name) {
        ensureProvidersLoaded();
        Supplier<TextureGenerator> supplier = NAMED_GENERATORS.get(name);
        if (supplier != null) {
            return supplier.get();
        }
        try {
            Class<?> clazz = Class.forName("com.oddlabs.tt.engine.procedural.Generator" + name);
            if (TextureGenerator.class.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked") Class<? extends TextureGenerator> genClass = (Class<
                        ? extends TextureGenerator>) clazz;
                Supplier<TextureGenerator> fallbackSupplier = () -> {
                    try {
                        return genClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to instantiate " + genClass, e);
                    }
                };
                NAMED_GENERATORS.put(name, fallbackSupplier);
                return fallbackSupplier.get();
            }
        } catch (ClassNotFoundException e) {
            // Expected when the named generator does not follow the Generator<Name> naming convention.
        }
        return null;
    }

    protected abstract Texture[] generate();

    @Override
    public final Texture[] get() {
        return generate();
    }

    @Override
    public int hashCode() {
        return getClass().getSimpleName().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return getClass().isInstance(o);
    }
}
