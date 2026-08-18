package com.techoptima.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create tables if they do not exist
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS transformation_budget ("
                    + "budget_id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "budget_amount DECIMAL(15, 2) NOT NULL,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")"
            );

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS applications ("
                    + "application_id BIGINT PRIMARY KEY,"
                    + "application_name VARCHAR(150) NOT NULL,"
                    + "modernization_cost DECIMAL(15, 2) NOT NULL,"
                    + "business_benefit INT NOT NULL,"
                    + "criticality ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,"
                    + "department ENUM('SALES', 'FINANCE', 'OPERATIONS', 'MARKETING', 'INFORMATION_TECHNOLOGY', 'HUMAN_RESOURCES', 'CUSTOMER_SERVICE') NOT NULL"
                    + ")"
            );

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS application_dependencies ("
                    + "application_id BIGINT NOT NULL,"
                    + "dependency_application_id BIGINT NOT NULL,"
                    + "PRIMARY KEY (application_id, dependency_application_id),"
                    + "FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE,"
                    + "FOREIGN KEY (dependency_application_id) REFERENCES applications(application_id) ON DELETE CASCADE"
                    + ")"
            );

            // 2. Check if applications table is empty; if so, populate initial seed dataset
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM applications")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO transformation_budget (budget_amount) VALUES (120.00)");
                    stmt.execute(
                            "INSERT INTO applications (application_id, application_name, modernization_cost, business_benefit, criticality, department) VALUES "
                            + "(1, 'User Identity & Directory Service', 15.00, 35, 'CRITICAL', 'INFORMATION_TECHNOLOGY'),"
                            + "(2, 'Core Payment Gateway', 20.00, 50, 'CRITICAL', 'FINANCE'),"
                            + "(3, 'Enterprise CRM Platform', 25.00, 60, 'HIGH', 'SALES'),"
                            + "(4, 'Automated Billing System', 18.00, 45, 'HIGH', 'FINANCE'),"
                            + "(5, 'Supply Chain & Inventory Portal', 22.00, 40, 'MEDIUM', 'OPERATIONS'),"
                            + "(6, 'Customer Support Helpdesk', 12.00, 30, 'MEDIUM', 'CUSTOMER_SERVICE'),"
                            + "(7, 'AI Business Analytics & Reporting', 30.00, 70, 'HIGH', 'MARKETING')"
                    );
                    stmt.execute(
                            "INSERT INTO application_dependencies (application_id, dependency_application_id) VALUES "
                            + "(2, 1), (3, 1), (4, 2), (5, 3), (6, 3), (7, 3), (7, 4)"
                    );
                    System.out.println("Database auto-initialized with schema and 7 enterprise sample applications.");
                }
            }

        } catch (SQLException exception) {
            System.err.println("Warning: Database auto-initialization could not complete: " + exception.getMessage());
        }
    }
}
