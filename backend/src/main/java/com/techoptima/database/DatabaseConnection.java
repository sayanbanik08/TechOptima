package com.techoptima.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                requiredEnvironment("TECHOPTIMA_DB_URL"),
                requiredEnvironment("TECHOPTIMA_DB_USER"),
                requiredEnvironment("TECHOPTIMA_DB_PASSWORD")
        );
    }

    private static String requiredEnvironment(
            String name)
            throws SQLException {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new SQLException(
                    "Database configuration is incomplete. Set "
                            + name
                            + " before starting the application."
            );
        }

        return value;
    }
}
