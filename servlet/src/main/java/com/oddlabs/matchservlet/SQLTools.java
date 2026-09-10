package com.oddlabs.matchservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper executing SQL actions and handling exceptions for servlets.
 */
final class SQLTools {
    private static final Logger logger = Logger.getLogger(SQLTools.class.getName());

    static void doSQL(HttpServletResponse res, SQLAction action) throws ServletException, IOException {
        try {
            action.run();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Database query execution error", e);
            res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }
}
