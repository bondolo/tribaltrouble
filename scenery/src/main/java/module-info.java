module com.oddlabs.tt.scenery {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.simulation;
    requires transitive com.oddlabs.tt.engine;
    requires com.oddlabs.tt.procedural;
    requires org.joml;
    requires static org.jspecify;
    requires org.lwjgl;
    requires org.lwjgl.opengl;

    exports com.oddlabs.tt.scenery;
}
