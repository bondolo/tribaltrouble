package com.oddlabs.net;

import java.io.IOException;
import org.jspecify.annotations.Nullable;

public interface ARMIArgumentReader {
  @Nullable Object readArgument(Class<?> type, ByteBufferInputStream in) throws IOException, ClassNotFoundException;
}
