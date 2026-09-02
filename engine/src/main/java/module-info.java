module com.oddlabs.tt.engine {
    requires transitive com.oddlabs.common;
    requires transitive com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.simulation;
    requires transitive com.oddlabs.tt.procedural;
    requires transitive com.oddlabs.tt.net;
    requires transitive com.oddlabs.tt.window;
    requires transitive com.oddlabs.tt.input;
    requires transitive com.oddlabs.tt.audio;
    requires transitive org.joml;
    requires static org.jspecify;
    requires java.desktop;
    requires java.logging;
    requires transitive org.lwjgl;
    requires transitive org.lwjgl.opengl;
    requires org.lwjgl.sdl;
    requires org.lwjgl.stb;

    exports com.oddlabs.tt.engine;
    exports com.oddlabs.tt.engine.cursor;
    exports com.oddlabs.tt.engine.font;
    exports com.oddlabs.tt.engine.image;
    exports com.oddlabs.tt.engine.procedural;
    exports com.oddlabs.tt.engine.render;
    exports com.oddlabs.tt.engine.render.scenery;
    exports com.oddlabs.tt.engine.render.shader;
    exports com.oddlabs.tt.engine.render.state;
    exports com.oddlabs.tt.engine.resource;
    exports com.oddlabs.tt.engine.settings;
    exports com.oddlabs.tt.engine.util;
    exports com.oddlabs.tt.engine.vbo;

    opens com.oddlabs.tt.engine.render to com.oddlabs.tt.base;

    uses com.oddlabs.tt.base.global.PropertiesSerializer;

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.engine.settings.AccessibilitySettings;
}
