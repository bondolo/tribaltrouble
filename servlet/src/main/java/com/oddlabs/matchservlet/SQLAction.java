package com.oddlabs.matchservlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Functional callback executing SQL database actions within servlet requests.
 */
interface SQLAction {
    void run() throws SQLException, ServletException, IOException;
}
