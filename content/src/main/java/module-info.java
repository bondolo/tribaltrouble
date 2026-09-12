module com.oddlabs.tt.content {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.tt.simulation;
    requires com.oddlabs.tt.procedural;
    requires com.oddlabs.tt.net;
    requires com.oddlabs.tt.window;
    requires com.oddlabs.tt.input;
    requires com.oddlabs.tt.audio;
    requires com.oddlabs.tt.engine;
    requires com.oddlabs.tt.gui;
    requires com.oddlabs.tt.client;
    requires static org.jspecify;
    requires java.logging;

    exports com.oddlabs.tt.content.form;
    exports com.oddlabs.tt.content.menu;
}
