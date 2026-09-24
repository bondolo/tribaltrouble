package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Race;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests verifying PlayerInfo serialization and state contracts.
 */
final class PlayerInfoTest {

    @Test
    void testSerializationRoundTrip() throws Exception {
        PlayerInfo original = new PlayerInfo(1, Race.VIKINGS, "Olaf");
        assertEquals(Race.VIKINGS, original.race());
        assertEquals(1, original.team());
        assertEquals("Olaf", original.name());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        PlayerInfo deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deserialized = (PlayerInfo) ois.readObject();
        }

        assertEquals(original, deserialized);
        assertEquals(Race.VIKINGS, deserialized.race());
        assertEquals(1, deserialized.team());
        assertEquals("Olaf", deserialized.name());
    }

    @Test
    void testNativesSerializationRoundTrip() throws Exception {
        PlayerInfo original = new PlayerInfo(0, Race.NATIVES, "Maku");
        assertEquals(Race.NATIVES, original.race());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        PlayerInfo deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deserialized = (PlayerInfo) ois.readObject();
        }

        assertEquals(original, deserialized);
        assertEquals(Race.NATIVES, deserialized.race());
        assertEquals(0, deserialized.team());
        assertEquals("Maku", deserialized.name());
    }
}
