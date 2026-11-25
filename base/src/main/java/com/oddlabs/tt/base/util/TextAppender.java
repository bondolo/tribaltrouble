package com.oddlabs.tt.base.util;

import org.jspecify.annotations.Nullable;

/// Interface for appending formatted text lines or fragments to text buffers or debug UI widgets.
@FunctionalInterface
public interface TextAppender {
  void append(@Nullable Object obj);
}
