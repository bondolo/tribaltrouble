module com.oddlabs.tt.content {
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
    requires transitive com.oddlabs.tt.client;
    requires transitive org.joml;
    requires static org.jspecify;
    requires java.desktop;
    requires java.xml;
    requires java.logging;
    requires org.lwjgl;
    requires org.lwjgl.sdl;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;

    exports com.oddlabs.tt.content.campaign;
    exports com.oddlabs.tt.content.form;
    exports com.oddlabs.tt.content.menu;
    exports com.oddlabs.tt.content.skirmish;
    exports com.oddlabs.tt.content.tutorial;
}
