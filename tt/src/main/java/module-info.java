module com.oddlabs.tt {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.tt.simulation;
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
            com.oddlabs.tt.settings.WindowSettings,
            com.oddlabs.tt.settings.AudioSettings,
            com.oddlabs.tt.settings.AccountSettings,
            com.oddlabs.tt.settings.AccessibilitySettings,
            com.oddlabs.tt.settings.ControlSettings;
}
