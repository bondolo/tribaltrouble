package com.oddlabs.tt.client.viewer;

import org.jspecify.annotations.NonNull;

public interface WorldInitAction {
    void run(@NonNull WorldViewer viewer);
}
