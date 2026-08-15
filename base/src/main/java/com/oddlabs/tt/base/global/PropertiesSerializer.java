package com.oddlabs.tt.base.global;

import org.jspecify.annotations.NonNull;

import java.util.Properties;

/** Loads and saves from the provided properties */
public interface PropertiesSerializer {
    void saveToProperties(@NonNull Properties props);

    void loadFromProperties(@NonNull Properties props);
}
