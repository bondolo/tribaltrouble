module com.oddlabs.tt.client {
    requires transitive com.oddlabs.tt.simulation;
    requires transitive com.oddlabs.tt.engine;
    requires com.oddlabs.tt.effects;
    requires com.oddlabs.tt.gui;
    requires static org.jspecify;
    requires java.xml;
    requires java.logging;
    requires org.lwjgl;
    requires org.lwjgl.sdl;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;

    exports com.oddlabs.tt.client.camera;
    exports com.oddlabs.tt.client.delegate;
    exports com.oddlabs.tt.client.gui;
    exports com.oddlabs.tt.client.render;
    exports com.oddlabs.tt.client.trigger;
    exports com.oddlabs.tt.client.viewer;

    opens com.oddlabs.tt.client.camera;
    opens com.oddlabs.tt.client.delegate;
    opens com.oddlabs.tt.client.gui;
    opens com.oddlabs.tt.client.render;
    opens com.oddlabs.tt.client.trigger;
    opens com.oddlabs.tt.client.viewer;
}
