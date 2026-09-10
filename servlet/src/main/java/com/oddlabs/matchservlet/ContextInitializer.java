package com.oddlabs.matchservlet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * Servlet context listener initializing the database connection pool and cryptographic keys.
 */
public final class ContextInitializer implements ServletContextListener {
    private static final int KEY_SIZE = 1024;
    private static final String KEY_ALGORITHM = "RSA";

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keygen.initialize(KEY_SIZE);
            return keygen.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute("db", createDatabasePool());
        KeyPair key_pair = generateKeyPair();
        ctx.setAttribute("private_key", key_pair.getPrivate());
        ctx.setAttribute("public_key", key_pair.getPublic());
    }

    @SuppressWarnings("BanJNDI")
    private static DataSource createDatabasePool() {
        try {
            Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
            return (DataSource) envCtx.lookup("jdbc/matchDB");
        } catch (NamingException e) {
            org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
            ds.setURL("jdbc:h2:mem:oddlabs;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
            ds.setUser("sa");
            ds.setPassword("");
            return ds;
        }
    }
}
