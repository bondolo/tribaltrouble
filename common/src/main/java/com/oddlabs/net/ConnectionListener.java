package com.oddlabs.net;

import org.jspecify.annotations.Nullable;

/// Listens for incoming network connections and manages their lifecycle.
public interface ConnectionListener extends AutoCloseable {
  AbstractConnection acceptConnection(@Nullable ConnectionInterface connection_interface);

  void rejectConnection();

  @Override
  void close();
}
