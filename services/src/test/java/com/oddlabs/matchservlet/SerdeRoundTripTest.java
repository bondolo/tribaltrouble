package com.oddlabs.matchservlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micronaut.json.JsonMapper;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/// Verifies Micronaut Serialization 3.x annotation processing and JSON round-tripping on Java 27.
@NullMarked
@MicronautTest(startApplication = false)
final class SerdeRoundTripTest {
  @Serdeable
  record SamplePayload(String name, int tick, List<String> tags, @Nullable String optionalNote) {}

  @Inject private JsonMapper jsonMapper;

  @Test
  void roundTripsRecordWithNullableFields() throws IOException {
    SamplePayload populated = new SamplePayload("viking", 42, List.of("alpha", "beta"), "note");
    String json = jsonMapper.writeValueAsString(populated);
    SamplePayload decoded = jsonMapper.readValue(json, SamplePayload.class);
    assertEquals(populated, decoded);

    SamplePayload withNull = new SamplePayload("native", 0, List.of(), null);
    String nullJson = jsonMapper.writeValueAsString(withNull);
    SamplePayload decodedNull = jsonMapper.readValue(nullJson, SamplePayload.class);
    assertEquals(withNull, decodedNull);
    assertNull(decodedNull.optionalNote());
  }
}
