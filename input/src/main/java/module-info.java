module com.oddlabs.tt.input {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.window;
    requires static org.jspecify;
    requires java.logging;
    requires transitive org.lwjgl;
    requires transitive org.lwjgl.sdl;

    exports com.oddlabs.tt.input;

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.input.ControlSettings,
            com.oddlabs.tt.input.InputManager;
}
