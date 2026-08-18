module com.oddlabs.tt {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.tt.simulation;
    requires com.oddlabs.tt.net;
    requires com.oddlabs.tt.window;
    requires com.oddlabs.tt.input;
    requires com.oddlabs.tt.audio;
    requires com.oddlabs.tt.audio.openal;
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

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.settings.AccessibilitySettings;
}
