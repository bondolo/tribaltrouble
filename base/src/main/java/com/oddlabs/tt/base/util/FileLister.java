package com.oddlabs.tt.base.util;

import com.oddlabs.event.Deterministic;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public final class FileLister implements FileListerInterface {
    private final FileListerListener listener;

    public FileLister(File dir, String pattern, FileListerListener listener,
            Deterministic deterministic) {
        this.listener = listener;
        newFiles(deterministic.log(dir.listFiles(new PatternFilenameFilter(
                pattern))));
    }

    @Override
    public void newFiles(File[] new_files) {
        listener.newFiles(new_files);
    }

    private record PatternFilenameFilter(String pattern) implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return Pattern.matches(pattern, name);
        }
    }
}
