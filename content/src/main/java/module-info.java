module com.oddlabs.tt.content {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.tt.simulation;
    requires com.oddlabs.tt.net;
    requires com.oddlabs.tt.window;
    requires com.oddlabs.tt.input;
    requires com.oddlabs.tt.audio;
    requires com.oddlabs.tt.engine;
    requires com.oddlabs.tt.gui;
    requires com.oddlabs.tt.client;
    requires org.joml;
    requires static org.jspecify;
    requires java.desktop;
    requires java.xml;
    requires java.logging;

    exports com.oddlabs.tt.content;
    exports com.oddlabs.tt.content.campaign;
    exports com.oddlabs.tt.content.form;
    exports com.oddlabs.tt.content.menu;
    exports com.oddlabs.tt.content.skirmish;
    exports com.oddlabs.tt.content.tutorial;

    opens com.oddlabs.tt.content;
    opens com.oddlabs.tt.content.campaign;
    opens com.oddlabs.tt.content.form;
    opens com.oddlabs.tt.content.menu;
    opens com.oddlabs.tt.content.skirmish;
    opens com.oddlabs.tt.content.tutorial;
}
