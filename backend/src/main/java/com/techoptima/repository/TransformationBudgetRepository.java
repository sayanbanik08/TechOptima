package com.techoptima.repository;

import com.techoptima.database.DatabaseConnection;
import com.techoptima.model.TransformationBudget;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class TransformationBudgetRepository {

    private static final String INSERT_SQL =
            "INSERT INTO transformation_budget (budget_amount) " +
            "VALUES (?)";

    private static final String FIND_LATEST_SQL =
            "SELECT budget_amount " +
            "FROM transformation_budget " +
            "ORDER BY budget_id DESC " +
            "LIMIT 1";

    private static final String DELETE_ALL_SQL =
            "DELETE FROM transformation_budget";

    public void save(TransformationBudget budget)
            throws SQLException {

        validateBudget(budget);

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT_SQL)) {

            statement.setBigDecimal(
                    1,
                    budget.getBudgetAmount()
            );

            statement.executeUpdate();
        }
    }

    public TransformationBudget findLatest()
            throws SQLException {

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(FIND_LATEST_SQL);
             ResultSet resultSet =
                     statement.executeQuery()) {

            if (!resultSet.next()) {
                return null;
            }

            BigDecimal amount =
                    resultSet.getBigDecimal("budget_amount");

            return new TransformationBudget(amount);
        }
    }

    /**
     * Replaces all stored budgets with one current budget.
     * Useful when the application supports one active
     * transformation budget at a time.
     */
    public void replaceCurrent(
            TransformationBudget budget)
            throws SQLException {

        validateBudget(budget);

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement deleteStatement =
                     connection.prepareStatement(DELETE_ALL_SQL);
             PreparedStatement insertStatement =
                     connection.prepareStatement(INSERT_SQL)) {

            connection.setAutoCommit(false);

            try {
                deleteStatement.executeUpdate();

                insertStatement.setBigDecimal(
                        1,
                        budget.getBudgetAmount()
                );

                insertStatement.executeUpdate();

                connection.commit();

            } catch (SQLException exception) {

                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void validateBudget(
            TransformationBudget budget) {

        if (budget == null) {
            throw new IllegalArgumentException(
                    "budget cannot be null"
            );
        }
    }
}