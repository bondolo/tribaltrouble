module com.oddlabs.tt.gui {
    requires transitive com.oddlabs.tt.engine;
    requires transitive com.oddlabs.tt.input;
    requires transitive com.oddlabs.tt.window;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.common;
    requires org.joml;
    requires static org.jspecify;
    requires org.lwjgl;
    requires org.lwjgl.opengl;
    requires java.desktop;
    requires java.xml;
    requires java.logging;

    exports com.oddlabs.tt.gui;
    exports com.oddlabs.tt.gui.event;
    exports com.oddlabs.tt.gui.render;
    exports com.oddlabs.tt.gui.delegate;
}
