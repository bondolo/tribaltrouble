package com.oddlabs.tt.core.util;

import java.io.File;
import java.io.IOException;

public interface FileLoaderListener {
    void error(IOException e);

    void data(byte[] data, int num_bytes, boolean eof);

    void newFile(File filename, long length);
}
