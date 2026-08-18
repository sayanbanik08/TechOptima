package com.techoptima.database;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        // 1. Check if direct TECHOPTIMA_* variables are configured
        String techoptimaUrl = System.getenv("TECHOPTIMA_DB_URL");
        String techoptimaUser = System.getenv("TECHOPTIMA_DB_USER");
        String techoptimaPassword = System.getenv("TECHOPTIMA_DB_PASSWORD");

        if (techoptimaUrl != null && !techoptimaUrl.isBlank()) {
            return DriverManager.getConnection(
                    techoptimaUrl,
                    techoptimaUser != null ? techoptimaUser : "",
                    techoptimaPassword != null ? techoptimaPassword : ""
            );
        }

        // 2. Check Railway / Cloud single URL variables (MYSQL_PRIVATE_URL, MYSQL_URL, DATABASE_URL)
        String rawUrl = getFirstNonBlank(
                "MYSQL_PRIVATE_URL",
                "MYSQL_URL",
                "DATABASE_URL"
        );

        if (rawUrl != null) {
            return getConnectionFromUrl(rawUrl);
        }

        // 3. Check Railway individual variables (MYSQLHOST, MYSQLUSER, MYSQLPASSWORD, etc.)
        String host = System.getenv("MYSQLHOST");
        if (host != null && !host.isBlank()) {
            String port = System.getenv("MYSQLPORT");
            String database = System.getenv("MYSQLDATABASE");
            String user = System.getenv("MYSQLUSER");
            String password = System.getenv("MYSQLPASSWORD");

            int portNum = (port != null && !port.isBlank()) ? Integer.parseInt(port) : 3306;
            String dbName = (database != null && !database.isBlank()) ? database : "railway";
            String jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host,
                    portNum,
                    dbName
            );

            return DriverManager.getConnection(
                    jdbcUrl,
                    user != null ? user : "root",
                    password != null ? password : ""
            );
        }

        throw new SQLException(
                "Database configuration is incomplete. Set TECHOPTIMA_DB_URL or MYSQL_PRIVATE_URL/MYSQL_URL before starting."
        );
    }

    private static Connection getConnectionFromUrl(String rawUrl) throws SQLException {
        if (rawUrl.startsWith("jdbc:mysql:")) {
            return DriverManager.getConnection(rawUrl);
        }

        try {
            // Handle mysql://user:pass@host:port/database format
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String path = uri.getPath();
            String dbName = (path != null && path.length() > 1) ? path.substring(1) : "railway";

            String user = "";
            String password = "";
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                user = parts[0];
                password = parts[1];
            } else if (userInfo != null) {
                user = userInfo;
            }

            String jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host,
                    port,
                    dbName
            );

            return DriverManager.getConnection(jdbcUrl, user, password);

        } catch (Exception e) {
            throw new SQLException("Failed to parse database connection URL: " + e.getMessage(), e);
        }
    }

    private static String getFirstNonBlank(String... varNames) {
        for (String varName : varNames) {
            String val = System.getenv(varName);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }
}
