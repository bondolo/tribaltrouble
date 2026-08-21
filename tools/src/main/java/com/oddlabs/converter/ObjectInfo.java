package com.oddlabs.converter;


import java.nio.file.Path;

public class ObjectInfo {
    private final Path file;

    public ObjectInfo(Path file) {
        this.file = file;
    }

    public final Path getFile() {
        return file;
    }
}
