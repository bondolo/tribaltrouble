module com.oddlabs.tt.scenery {
    requires transitive com.oddlabs.common;
    requires transitive com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.simulation;
    requires transitive com.oddlabs.tt.engine;
    requires com.oddlabs.tt.procedural;
    requires transitive org.joml;
    requires static org.jspecify;
    requires transitive org.lwjgl;
    requires transitive org.lwjgl.opengl;

    exports com.oddlabs.tt.scenery;
}
