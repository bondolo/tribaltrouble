package com.oddlabs.tt.base.util;

import java.io.File;

public interface FileListerListener {
    void newFiles(File[] files);
}
