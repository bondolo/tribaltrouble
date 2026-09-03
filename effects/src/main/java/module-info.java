module com.oddlabs.tt.effects {
    requires transitive com.oddlabs.tt.engine;
    requires static org.jspecify;
    requires org.lwjgl;
    requires org.lwjgl.opengl;
    requires java.logging;

    exports com.oddlabs.tt.effects.particle;
    exports com.oddlabs.tt.effects.render;
}
