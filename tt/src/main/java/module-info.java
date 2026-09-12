module com.oddlabs.tt {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.tt.net;
    requires com.oddlabs.tt.window;
    requires com.oddlabs.tt.input;
    requires com.oddlabs.tt.audio;
    requires com.oddlabs.tt.audio.openal;
    requires com.oddlabs.tt.engine;
    requires com.oddlabs.tt.gui;
    requires com.oddlabs.tt.client;
    requires com.oddlabs.tt.content;
    requires static org.jspecify;
    requires java.logging;
    requires org.lwjgl;
    requires org.lwjgl.sdl;

    uses com.oddlabs.tt.base.global.PropertiesSerializer;
}
