package com.oddlabs.util;

import com.oddlabs.event.Deterministic;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Serializes and deserializes objects deterministically with support for class alias migrations.
 */
public final class DeterministicSerializer {
    private DeterministicSerializer() {
    }

    public static <T> void save(Deterministic deterministic, final Object object, final Path file,
            final DeterministicSerializerLoopbackInterface<T> callback_loopback) {
        @Nullable IOException exception = null;
        try (ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(file))) {
            os.writeObject(object);
        } catch (IOException e) {
            exception = e;
        }
        if (deterministic.log(exception != null)) {
            callback_loopback.failed(Objects.requireNonNull(deterministic.log(exception)));
        } else {
            callback_loopback.saveSucceeded();
        }
    }

    public static <T> void load(Deterministic deterministic, final Path file,
            final DeterministicSerializerLoopbackInterface<T> callback_loopback) {
        load(deterministic, file, callback_loopback, Map.of());
    }

    public static <T> void load(Deterministic deterministic, final Path file,
            final DeterministicSerializerLoopbackInterface<T> callback_loopback,
            Map<String, String> classAliases) {
        @Nullable T object = null;
        @Nullable Throwable throwable = null;
        try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(file)) {
            @Override
            protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
                ObjectStreamClass desc = super.readClassDescriptor();
                String name = desc.getName();
                String target = classAliases.get(name);
                if (target != null) {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    Class<?> cl = Class.forName(target, false, loader != null ? loader : getClass().getClassLoader());
                    return ObjectStreamClass.lookup(cl);
                }
                if (name.startsWith("[L") && name.endsWith(";")) {
                    String elementClass = name.substring(2, name.length() - 1);
                    String mappedElement = classAliases.get(elementClass);
                    if (mappedElement != null) {
                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        Class<?> cl = Class.forName("[L" + mappedElement + ";", false,
                                loader != null ? loader : getClass().getClassLoader());
                        return ObjectStreamClass.lookup(cl);
                    }
                }
                return desc;
            }

            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                try {
                    return super.resolveClass(desc);
                } catch (ClassNotFoundException e) {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    if (loader != null) {
                        return Class.forName(desc.getName(), false, loader);
                    }
                    throw e;
                }
            }
        }) {
            //noinspection unchecked
            object = (T) is.readObject();
        } catch (Throwable all) {
            throwable = all;
        }
        if (deterministic.log(throwable != null)) {
            callback_loopback.failed(Objects.requireNonNull(deterministic.log(throwable)));
        } else {
            callback_loopback.loadSucceeded(Objects.requireNonNull(deterministic.log(object)));
        }
    }
}
