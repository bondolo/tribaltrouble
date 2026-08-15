module com.oddlabs.tt.base {
    requires transitive com.oddlabs.common;
    requires static org.jspecify;
    requires java.desktop;
    requires java.logging;

    exports com.oddlabs.tt.base.animation;
    exports com.oddlabs.tt.base.event;
    exports com.oddlabs.tt.base.global;
    exports com.oddlabs.tt.base.util;
}
