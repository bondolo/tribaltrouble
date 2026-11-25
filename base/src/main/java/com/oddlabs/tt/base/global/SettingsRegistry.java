package com.oddlabs.tt.base.global;

import java.util.NoSuchElementException;

/// Registry for retrieving modular settings components by type.
public interface SettingsRegistry {
  <T extends PropertiesSerializer> T get(Class<T> type) throws NoSuchElementException;
}
