package com.oddlabs.tt.core.util;

import java.io.File;

public interface FileListerListener {
    void newFiles(File[] files);
}
