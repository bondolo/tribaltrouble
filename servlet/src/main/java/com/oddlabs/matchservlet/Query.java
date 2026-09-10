package com.oddlabs.matchservlet;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQL result set processor strategy.
 */
interface Query {
    Object process(ResultSet result) throws SQLException;
}
