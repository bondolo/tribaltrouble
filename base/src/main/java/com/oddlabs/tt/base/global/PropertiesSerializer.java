package com.oddlabs.tt.base.global;


import java.util.Properties;

/** Loads and saves from the provided properties */
public interface PropertiesSerializer {
    void saveToProperties(Properties props);

    void loadFromProperties(Properties props);
}
