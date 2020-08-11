package com.oddlabs.http;

import java.io.IOException;

public strictfp interface HttpCallback {
	void success(Object result);
    default void error(int error_code, String error_message) {
        error(new IOException("HTTP error code: " + error_code + " " + error_message));
    }
	void error(IOException e);
}
