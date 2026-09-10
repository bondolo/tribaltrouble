/**
 * Standalone servers for matchmaking and router forwarding.
 */
module com.oddlabs.server {
    requires com.oddlabs.common;
    requires java.sql;
    requires java.naming;
    requires java.logging;
    requires static org.jspecify;
    requires com.h2database;

    exports com.oddlabs.matchserver;
    exports com.oddlabs.routerserver;
}
