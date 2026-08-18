package com.techoptima.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {

    @Test
    void shouldEstablishDatabaseConnection() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {

            assertNotNull(connection);
            assertFalse(connection.isClosed());
            assertEquals("techoptima", connection.getCatalog());
        }
    }
}
