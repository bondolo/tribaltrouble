package com.oddlabs.procedural;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link MapCode} encoding and decoding.
 */
class MapCodeTest {

    @Test
    void testEncodeDecodeRoundTrip() {
        List<MapParameters.SlotSetting> otherSlots = List.of(
                new MapParameters.SlotSetting(1, 0, 1),
                new MapParameters.SlotSetting(2, 1, 1),
                new MapParameters.SlotSetting(3, 0, 2),
                new MapParameters.SlotSetting(0, 1, 0),
                new MapParameters.SlotSetting(0, 0, 0)
        );

        MapParameters original = new MapParameters(
                12345,
                7,
                4,
                8,
                1,
                2,
                1,
                0,
                otherSlots
        );

        String encoded = MapCode.encode(original);
        assertNotNull(encoded);

        MapParameters decoded = MapCode.decode(encoded);

        assertEquals(original.seed(), decoded.seed());
        assertEquals(original.hills(), decoded.hills());
        assertEquals(original.vegetation(), decoded.vegetation());
        assertEquals(original.supplies(), decoded.supplies());
        assertEquals(original.terrainType(), decoded.terrainType());
        assertEquals(original.size(), decoded.size());
        assertEquals(original.player0Race(), decoded.player0Race());
        assertEquals(original.player0Team(), decoded.player0Team());
        assertEquals(original.otherSlots().size(), decoded.otherSlots().size());

        for (int i = 0; i < original.otherSlots().size(); i++) {
            assertEquals(original.otherSlots().get(i), decoded.otherSlots().get(i));
        }

        // Test re-encoding produces identical string
        assertEquals(encoded, MapCode.encode(decoded));
    }

    @Test
    void testStandardMapCodeDecodes() {
        String testCode = "4Y4SDR388K";
        MapParameters params = MapCode.decode(testCode);

        assertNotNull(params);
        assertEquals(5, params.otherSlots().size());
    }
}
