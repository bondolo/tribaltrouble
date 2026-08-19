module com.oddlabs.tt {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.tt.simulation;
    requires com.oddlabs.tt.procedural;
    requires com.oddlabs.tt.net;
    requires com.oddlabs.tt.window;
    requires com.oddlabs.tt.input;
    requires com.oddlabs.tt.audio;
    requires com.oddlabs.tt.audio.openal;
    requires com.oddlabs.tt.engine;
    requires com.oddlabs.tt.effects;
    requires com.oddlabs.tt.gui;
    requires org.joml;
    requires static org.jspecify;
    requires java.desktop;
    requires java.xml;
    requires java.logging;
    requires org.lwjgl;
    requires org.lwjgl.sdl;
    requires org.lwjgl.openal;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
    requires kotlin.stdlib;

    uses com.oddlabs.tt.base.global.PropertiesSerializer;
}
