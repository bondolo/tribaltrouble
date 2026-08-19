module com.oddlabs.tt.client {
    requires transitive com.oddlabs.common;
    requires transitive com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.simulation;
    requires transitive com.oddlabs.tt.procedural;
    requires transitive com.oddlabs.tt.net;
    requires transitive com.oddlabs.tt.window;
    requires transitive com.oddlabs.tt.input;
    requires transitive com.oddlabs.tt.audio;
    requires transitive com.oddlabs.tt.engine;
    requires transitive com.oddlabs.tt.effects;
    requires transitive com.oddlabs.tt.gui;
    requires transitive org.joml;
    requires static org.jspecify;
    requires java.desktop;
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
}
