package com.techoptima.repository;

import com.techoptima.database.DatabaseConnection;
import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class ApplicationRepository {

    private static final String FIND_ALL_APPLICATIONS_SQL =
            "SELECT application_id, application_name, modernization_cost, " +
            "business_benefit, criticality, department " +
            "FROM applications ORDER BY application_id";

    private static final String FIND_ALL_DEPENDENCIES_SQL =
            "SELECT application_id, dependency_application_id " +
            "FROM application_dependencies " +
            "ORDER BY application_id, dependency_application_id";

    private static final String FIND_BY_ID_SQL =
            "SELECT application_id, application_name, modernization_cost, " +
            "business_benefit, criticality, department " +
            "FROM applications WHERE application_id = ?";

    private static final String DELETE_BY_ID_SQL =
            "DELETE FROM applications WHERE application_id = ?";

    private static final String INSERT_SQL =
            "INSERT INTO applications " +
            "(application_id, application_name, modernization_cost, " +
            "business_benefit, criticality, department) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE applications SET " +
            "application_name = ?, modernization_cost = ?, " +
            "business_benefit = ?, criticality = ?, department = ? " +
            "WHERE application_id = ?";

    private static final String DELETE_DEPENDENCIES_SQL =
            "DELETE FROM application_dependencies WHERE application_id = ?";

    private static final String DELETE_ALL_DEPENDENCIES_FOR_APP_SQL =
            "DELETE FROM application_dependencies " +
            "WHERE application_id = ? OR dependency_application_id = ?";

    private static final String FIND_DEPENDENCIES_BY_APPLICATION_ID_SQL =
            "SELECT dependency_application_id " +
            "FROM application_dependencies " +
            "WHERE application_id = ? " +
            "ORDER BY dependency_application_id";

    private static final String INSERT_DEPENDENCY_SQL =
            "INSERT INTO application_dependencies " +
            "(application_id, dependency_application_id) " +
            "VALUES (?, ?)";

    public List<Application> findAll() throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection()) {

            Map<Long, List<Long>> dependenciesByApplication =
                    loadAllDependencies(connection);

            List<Application> applications = new ArrayList<>();

            try (PreparedStatement statement =
                         connection.prepareStatement(FIND_ALL_APPLICATIONS_SQL);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    applications.add(
                            mapApplication(
                                    resultSet,
                                    dependenciesByApplication.getOrDefault(
                                            resultSet.getLong("application_id"),
                                            List.of()
                                    )
                            )
                    );
                }
            }

            return applications;
        }
    }

    public Application findById(long applicationId)
            throws SQLException {

        if (applicationId <= 0) {
            throw new IllegalArgumentException(
                    "applicationId must be greater than 0"
            );
        }

        try (Connection connection = DatabaseConnection.getConnection()) {

            List<Long> dependencies =
                    loadDependencies(connection, applicationId);

            try (PreparedStatement statement =
                         connection.prepareStatement(FIND_BY_ID_SQL)) {

                statement.setLong(1, applicationId);

                try (ResultSet resultSet = statement.executeQuery()) {

                    if (!resultSet.next()) {
                        return null;
                    }

                    return mapApplication(
                            resultSet,
                            dependencies
                    );
                }
            }
        }
    }

    public void save(Application application)
            throws SQLException {

        validateApplication(application);

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT_SQL);
             PreparedStatement dependencyStatement =
                     connection.prepareStatement(INSERT_DEPENDENCY_SQL)) {

            connection.setAutoCommit(false);

            try {
                bindApplication(statement, application);
                statement.executeUpdate();

                insertDependencies(
                        dependencyStatement,
                        application
                );

                connection.commit();

            } catch (SQLException exception) {

                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void update(Application application)
            throws SQLException {

        validateApplication(application);

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement updateStatement =
                     connection.prepareStatement(UPDATE_SQL);
             PreparedStatement deleteDependencyStatement =
                     connection.prepareStatement(DELETE_DEPENDENCIES_SQL);
             PreparedStatement insertDependencyStatement =
                     connection.prepareStatement(INSERT_DEPENDENCY_SQL)) {

            connection.setAutoCommit(false);

            try {
                bindApplicationForUpdate(
                        updateStatement,
                        application
                );

                int rowsUpdated =
                        updateStatement.executeUpdate();

                if (rowsUpdated == 0) {
                    throw new IllegalArgumentException(
                            "Application not found: "
                                    + application.getApplicationId()
                    );
                }

                deleteDependencyStatement.setLong(
                        1,
                        application.getApplicationId()
                );

                deleteDependencyStatement.executeUpdate();

                insertDependencies(
                        insertDependencyStatement,
                        application
                );

                connection.commit();

            } catch (SQLException | RuntimeException exception) {

                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean deleteById(long applicationId)
            throws SQLException {

        if (applicationId <= 0) {
            throw new IllegalArgumentException(
                    "applicationId must be greater than 0"
            );
        }

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try (PreparedStatement deleteDepsStatement =
                         connection.prepareStatement(
                                 DELETE_ALL_DEPENDENCIES_FOR_APP_SQL
                         );
                 PreparedStatement deleteAppStatement =
                         connection.prepareStatement(DELETE_BY_ID_SQL)) {

                deleteDepsStatement.setLong(1, applicationId);
                deleteDepsStatement.setLong(2, applicationId);
                deleteDepsStatement.executeUpdate();

                deleteAppStatement.setLong(1, applicationId);
                int rows = deleteAppStatement.executeUpdate();

                connection.commit();

                return rows > 0;

            } catch (SQLException exception) {

                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private Map<Long, List<Long>> loadAllDependencies(
            Connection connection)
            throws SQLException {

        Map<Long, List<Long>> result =
                new HashMap<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             FIND_ALL_DEPENDENCIES_SQL
                     );
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {

                long applicationId =
                        resultSet.getLong("application_id");

                long dependencyId =
                        resultSet.getLong(
                                "dependency_application_id"
                        );

                result.computeIfAbsent(
                        applicationId,
                        ignored -> new ArrayList<>()
                ).add(dependencyId);
            }
        }

        return result;
    }

    private List<Long> loadDependencies(
            Connection connection,
            long applicationId)
            throws SQLException {

        List<Long> dependencies =
                new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             FIND_DEPENDENCIES_BY_APPLICATION_ID_SQL
                     )) {

            statement.setLong(1, applicationId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    dependencies.add(
                            resultSet.getLong(
                                    "dependency_application_id"
                            )
                    );
                }
            }
        }

        return dependencies;
    }

    private void insertDependencies(
            PreparedStatement statement,
            Application application)
            throws SQLException {

        for (Long dependencyId :
                new HashSet<>(
                        application.getDependencyApplicationIds()
                )) {

            statement.setLong(
                    1,
                    application.getApplicationId()
            );

            statement.setLong(
                    2,
                    dependencyId
            );

            statement.addBatch();
        }

        statement.executeBatch();
    }

    private Application mapApplication(
            ResultSet resultSet,
            List<Long> dependencyIds)
            throws SQLException {

        return new Application(
                resultSet.getLong("application_id"),
                resultSet.getString("application_name"),
                resultSet.getBigDecimal("modernization_cost"),
                resultSet.getInt("business_benefit"),
                Criticality.valueOf(
                        resultSet.getString("criticality")
                ),
                Department.valueOf(
                        resultSet.getString("department")
                ),
                dependencyIds
        );
    }

    private void bindApplication(
            PreparedStatement statement,
            Application application)
            throws SQLException {

        statement.setLong(
                1,
                application.getApplicationId()
        );

        statement.setString(
                2,
                application.getApplicationName()
        );

        statement.setBigDecimal(
                3,
                application.getModernizationCost()
        );

        statement.setInt(
                4,
                application.getBusinessBenefit()
        );

        statement.setString(
                5,
                application.getCriticality().name()
        );

        statement.setString(
                6,
                application.getDepartment().name()
        );
    }

    private void bindApplicationForUpdate(
            PreparedStatement statement,
            Application application)
            throws SQLException {

        statement.setString(
                1,
                application.getApplicationName()
        );

        statement.setBigDecimal(
                2,
                application.getModernizationCost()
        );

        statement.setInt(
                3,
                application.getBusinessBenefit()
        );

        statement.setString(
                4,
                application.getCriticality().name()
        );

        statement.setString(
                5,
                application.getDepartment().name()
        );

        statement.setLong(
                6,
                application.getApplicationId()
        );
    }

    private void validateApplication(
            Application application) {

        if (application == null) {
            throw new IllegalArgumentException(
                    "application cannot be null"
            );
        }
    }
}