package com.oddlabs.matchservlet;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Query handler extracting the first string column from a SQL result set.
 */
final class GetFirstStringQuery implements Query {
    @Override
    public Object process(ResultSet result) throws SQLException {
        result.first();
        return result.getString(1);
    }
}
