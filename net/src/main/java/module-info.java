module com.oddlabs.tt.net {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.simulation;
    requires static org.jspecify;
    requires java.logging;

    exports com.oddlabs.tt.net;

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.net.AccountSettings;
}
