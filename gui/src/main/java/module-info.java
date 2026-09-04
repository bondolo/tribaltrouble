module com.oddlabs.tt.gui {
    requires transitive com.oddlabs.tt.engine;
    requires static org.jspecify;
    requires java.xml;
    requires java.logging;

    exports com.oddlabs.tt.gui;
    exports com.oddlabs.tt.gui.event;
    exports com.oddlabs.tt.gui.render;
    exports com.oddlabs.tt.gui.delegate;

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.gui.GUISettings;
}
