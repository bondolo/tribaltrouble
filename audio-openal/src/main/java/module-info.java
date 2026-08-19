module com.oddlabs.tt.audio.openal {
    requires transitive com.oddlabs.tt.audio;
    requires com.oddlabs.tt.base;
    requires com.oddlabs.common;
    requires org.joml;
    requires static org.jspecify;
    requires java.desktop;
    requires java.logging;
    requires org.lwjgl;
    requires org.lwjgl.openal;
    requires org.lwjgl.stb;

    exports com.oddlabs.tt.audio.openal;
}
