module com.oddlabs.tt.base {
    requires transitive com.oddlabs.common;
    requires static org.jspecify;
    requires java.logging;

    exports com.oddlabs.tt.base.animation;
    exports com.oddlabs.tt.base.event;
    exports com.oddlabs.tt.base.global;
    exports com.oddlabs.tt.base.geom;
    exports com.oddlabs.tt.base.resource;
    exports com.oddlabs.tt.base.util;

    uses com.oddlabs.tt.base.global.PropertiesSerializer;

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.base.global.LocaleSettings;
}
