package com.oddlabs.util;

import com.oddlabs.event.NotDeterministic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DeterministicSerializer}.
 */
final class DeterministicSerializerTest {

    static final class TargetState implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String data;

        TargetState(String data) {
            this.data = data;
        }

        String data() {
            return data;
        }
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) {
        Path file = tempDir.resolve("state.dat");
        TargetState original = new TargetState("hello");
        NotDeterministic deterministic = new NotDeterministic();

        AtomicBoolean saved = new AtomicBoolean(false);
        DeterministicSerializer.save(deterministic, original, file, new DeterministicSerializerLoopbackInterface<>() {
            @Override
            public void saveSucceeded() {
                saved.set(true);
            }

            @Override
            public void failed(Throwable e) {
                throw new AssertionError(e);
            }
        });
        assertTrue(saved.get(), "Save should succeed");

        AtomicReference<TargetState> loaded = new AtomicReference<>();
        DeterministicSerializer.load(deterministic, file, new DeterministicSerializerLoopbackInterface<TargetState>() {
            @Override
            public void loadSucceeded(TargetState object) {
                loaded.set(object);
            }

            @Override
            public void failed(Throwable e) {
                throw new AssertionError(e);
            }
        });

        assertNotNull(loaded.get());
        assertEquals("hello", loaded.get().data());
    }

    @Test
    void loadWithClassAliasMigration(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("aliased.dat");

        // Write a serialized byte stream with a fake legacy class name
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(new TargetState("migrated-payload"));
        }
        byte[] bytes = baos.toByteArray();

        // Replace "TargetState" with "LegacyState" in stream bytes to simulate old class
        String targetName = TargetState.class.getName();
        String legacyName = targetName.replace("TargetState", "LegacyState");
        assertEquals(targetName.length(), legacyName.length());

        byte[] modifiedBytes = bytes.clone();
        byte[] targetBytes = targetName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] legacyBytes = legacyName.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        boolean replaced = false;
        for (int i = 0; i <= modifiedBytes.length - targetBytes.length; i++) {
            boolean match = true;
            for (int j = 0; j < targetBytes.length; j++) {
                if (modifiedBytes[i + j] != targetBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                System.arraycopy(legacyBytes, 0, modifiedBytes, i, legacyBytes.length);
                replaced = true;
                break;
            }
        }
        assertTrue(replaced, "Target class name should be found and replaced in serialized bytes");

        Files.write(file, modifiedBytes);

        NotDeterministic deterministic = new NotDeterministic();
        AtomicReference<TargetState> loaded = new AtomicReference<>();

        Map<String, String> aliases = Map.of(legacyName, targetName);
        DeterministicSerializer.load(deterministic, file, new DeterministicSerializerLoopbackInterface<TargetState>() {
            @Override
            public void loadSucceeded(TargetState object) {
                loaded.set(object);
            }

            @Override
            public void failed(Throwable e) {
                throw new AssertionError("Load with alias should have succeeded", e);
            }
        }, aliases);

        assertNotNull(loaded.get());
        assertEquals("migrated-payload", loaded.get().data());
    }
}
