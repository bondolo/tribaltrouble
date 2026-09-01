package com.oddlabs.net;

import java.util.concurrent.TimeUnit;

/** A monotonic elapsed milliseconds clock. */
@FunctionalInterface
public interface TimeManager {

    /** A default implementation that uses {@link System#nanoTime()}. */
    TimeManager DEFAULT = () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

    /** {@return monotonically increasing elapsed milliseconds since some epoch.} */
    long getMillis();
}
