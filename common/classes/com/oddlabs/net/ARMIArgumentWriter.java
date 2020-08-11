package com.oddlabs.net;

import com.oddlabs.util.ByteBufferOutputStream;
import java.io.*;

public strictfp interface ARMIArgumentWriter {
	void writeArgument(Class<?> type, Object arg, ByteBufferOutputStream out) throws IOException;
}
