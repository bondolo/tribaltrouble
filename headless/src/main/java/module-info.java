module com.oddlabs.tt.headless {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.tt.simulation;
    requires com.oddlabs.tt.procedural;
    requires com.oddlabs.tt.ai;
    requires com.oddlabs.tt.net;
    requires static org.jspecify;
    requires java.logging;

    exports com.oddlabs.tt.headless;
}
