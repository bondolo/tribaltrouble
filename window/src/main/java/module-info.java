module com.oddlabs.tt.window {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires transitive org.joml;
    requires static org.jspecify;
    requires java.logging;
    requires transitive org.lwjgl;
    requires transitive org.lwjgl.sdl;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;

    exports com.oddlabs.tt.window;

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.window.WindowSettings;
}
