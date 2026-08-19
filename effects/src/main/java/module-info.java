module com.oddlabs.tt.effects {
    requires transitive com.oddlabs.tt.engine;
    requires transitive com.oddlabs.tt.simulation;
    requires com.oddlabs.tt.audio;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.common;
    requires org.joml;
    requires static org.jspecify;
    requires org.lwjgl;
    requires org.lwjgl.opengl;
    requires java.desktop;
    requires java.logging;

    exports com.oddlabs.tt.effects.particle;
    exports com.oddlabs.tt.effects.render;
}
