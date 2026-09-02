module com.oddlabs.tt.audio {
    requires transitive com.oddlabs.tt.base;
    requires transitive com.oddlabs.common;
    requires transitive org.joml;
    requires static org.jspecify;
    requires java.logging;
    requires org.lwjgl;
    requires org.lwjgl.stb;

    exports com.oddlabs.tt.audio;

    uses com.oddlabs.tt.audio.AudioProvider;

    provides com.oddlabs.tt.base.global.PropertiesSerializer with
            com.oddlabs.tt.audio.AudioSettings;
}
