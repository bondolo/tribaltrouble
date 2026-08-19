module com.oddlabs.tt.procedural {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.simulation;
    requires org.joml;
    requires org.jspecify;
    requires org.lwjgl;
    requires org.lwjgl.opengl;
    requires org.lwjgl.stb;
    requires java.desktop;
    requires java.logging;

    exports com.oddlabs.tt.procedural;
}
