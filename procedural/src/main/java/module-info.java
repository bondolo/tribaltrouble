module com.oddlabs.tt.procedural {
    requires com.oddlabs.common;
    requires com.oddlabs.tt.base;
    requires transitive com.oddlabs.tt.simulation;
    requires org.joml;
    requires static org.jspecify;
    requires java.logging;

    exports com.oddlabs.tt.procedural.noise;
    exports com.oddlabs.tt.procedural.erosion;
    exports com.oddlabs.tt.procedural.shape;
    exports com.oddlabs.tt.procedural.landscape;
    exports com.oddlabs.tt.procedural.image;
}
