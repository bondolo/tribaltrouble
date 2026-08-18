module com.oddlabs.tt.simulation {
    requires transitive com.oddlabs.common;
    requires transitive com.oddlabs.tt.base;
    requires transitive org.joml;
    requires static org.jspecify;

    exports com.oddlabs.tt.simulation;
    exports com.oddlabs.tt.simulation.behaviour;
    exports com.oddlabs.tt.simulation.landscape;
    exports com.oddlabs.tt.simulation.model;
    exports com.oddlabs.tt.simulation.model.weapon;
    exports com.oddlabs.tt.simulation.pathfinder;
    exports com.oddlabs.tt.simulation.player;
    exports com.oddlabs.tt.simulation.trigger;
    exports com.oddlabs.tt.simulation.util;

    opens com.oddlabs.tt.simulation.landscape;
    opens com.oddlabs.tt.simulation.model;
    opens com.oddlabs.tt.simulation.player;
}
