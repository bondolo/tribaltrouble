module com.oddlabs.tt.client {
    requires transitive com.oddlabs.common;
    requires transitive com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.simulation;
    requires transitive com.oddlabs.tt.net;
    requires transitive com.oddlabs.tt.window;
    requires transitive com.oddlabs.tt.audio;
    requires transitive com.oddlabs.tt.engine;
    requires com.oddlabs.tt.effects;
    requires com.oddlabs.tt.gui;
    requires static org.jspecify;
    requires java.desktop;
    requires java.logging;
    requires org.lwjgl;
    requires org.lwjgl.opengl;

    exports com.oddlabs.tt.client;
    exports com.oddlabs.tt.client.camera;
    exports com.oddlabs.tt.client.delegate;
    exports com.oddlabs.tt.client.gui;
    exports com.oddlabs.tt.client.render;
    exports com.oddlabs.tt.client.trigger;
    exports com.oddlabs.tt.client.viewer;

    opens com.oddlabs.tt.client;
    opens com.oddlabs.tt.client.camera;
    opens com.oddlabs.tt.client.delegate;
    opens com.oddlabs.tt.client.gui;
    opens com.oddlabs.tt.client.render;
    opens com.oddlabs.tt.client.trigger;
    opens com.oddlabs.tt.client.viewer;

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.client.camera.CameraSettings,
            com.oddlabs.tt.client.GameplaySettings;
}
