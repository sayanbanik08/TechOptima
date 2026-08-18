package com.techoptima.service;

import com.techoptima.database.DatabaseConnection;
import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;
import com.techoptima.model.TransformationBudget;
import com.techoptima.repository.ApplicationRepository;
import com.techoptima.repository.TransformationBudgetRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioOptimizationServiceTest {

    private static final long CRM_ID = 910001L;
    private static final long ANALYTICS_ID = 910002L;
    private static final long ERP_ID = 910003L;
    private static final long BILLING_ID = 910004L;

    private final ApplicationRepository applicationRepository =
            new ApplicationRepository();

    private final TransformationBudgetRepository budgetRepository =
            new TransformationBudgetRepository();

    private final PortfolioOptimizationService service =
            new PortfolioOptimizationService(
                    applicationRepository,
                    budgetRepository
            );

    private TransformationBudget originalBudget;

    @BeforeEach
    void cleanBefore() throws Exception {
        originalBudget = budgetRepository.findLatest();
        cleanup();
        insertDatabaseTestData();
    }

    @AfterEach
    void cleanAfter() throws Exception {
        cleanup();
        if (originalBudget != null) {
            budgetRepository.replaceCurrent(originalBudget);
        }
    }

    @Test
    void shouldRunCompleteOptimizationUsingRealDatabaseData()
            throws Exception {

        OptimizationResult result =
                service.optimize();

        assertTrue(result.isSuccessful());

        assertEquals(
                new BigDecimal("45.00"),
                result.getBudget().getBudgetAmount()
        );

        assertEquals(
                170,
                result.getKnapsackResult()
                        .getTotalBusinessBenefit()
        );

        assertEquals(
                new BigDecimal("45.00"),
                result.getKnapsackResult()
                        .getTotalCost()
        );

        List<Application> selected =
                result.getKnapsackResult()
                        .getSelectedApplications();

        assertEquals(2, selected.size());

        assertEquals(
                List.of(
                        "CRM",
                        "Analytics"
                ),
                selected.stream()
                        .map(Application::getApplicationName)
                        .collect(java.util.stream.Collectors.toList())
        );

        assertTrue(
                result.getDependencyValidationResult()
                        .isValid()
        );

        assertEquals(
                List.of(
                        "CRM",
                        "Analytics"
                ),
                result.getTopologicalSortResult()
                        .getOrderedApplications()
                        .stream()
                        .map(Application::getApplicationName)
                        .collect(java.util.stream.Collectors.toList())
        );
    }

    private void insertDatabaseTestData()
            throws Exception {

        budgetRepository.replaceCurrent(
                new TransformationBudget(
                        new BigDecimal("45.00")
                )
        );

        applicationRepository.save(
                application(
                        CRM_ID,
                        "CRM",
                        "20.00",
                        80,
                        Criticality.HIGH,
                        Department.SALES,
                        List.of()
                )
        );

        applicationRepository.save(
                application(
                        ANALYTICS_ID,
                        "Analytics",
                        "25.00",
                        90,
                        Criticality.HIGH,
                        Department.INFORMATION_TECHNOLOGY,
                        List.of(CRM_ID)
                )
        );

        applicationRepository.save(
                application(
                        ERP_ID,
                        "ERP",
                        "40.00",
                        60,
                        Criticality.CRITICAL,
                        Department.OPERATIONS,
                        List.of()
                )
        );

        applicationRepository.save(
                application(
                        BILLING_ID,
                        "Billing",
                        "18.00",
                        50,
                        Criticality.MEDIUM,
                        Department.FINANCE,
                        List.of()
                )
        );
    }

    private Application application(
            long id,
            String name,
            String cost,
            int benefit,
            Criticality criticality,
            Department department,
            List<Long> dependencies) {

        return new Application(
                id,
                name,
                new BigDecimal(cost),
                benefit,
                criticality,
                department,
                dependencies
        );
    }

    private void cleanup() throws Exception {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM application_dependencies " +
                                 "WHERE application_id IN (?, ?, ?, ?) " +
                                 "OR dependency_application_id IN (?, ?, ?, ?)"
                         )) {

                statement.setLong(1, CRM_ID);
                statement.setLong(2, ANALYTICS_ID);
                statement.setLong(3, ERP_ID);
                statement.setLong(4, BILLING_ID);

                statement.setLong(5, CRM_ID);
                statement.setLong(6, ANALYTICS_ID);
                statement.setLong(7, ERP_ID);
                statement.setLong(8, BILLING_ID);

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM applications " +
                                 "WHERE application_id IN (?, ?, ?, ?)"
                         )) {

                statement.setLong(1, CRM_ID);
                statement.setLong(2, ANALYTICS_ID);
                statement.setLong(3, ERP_ID);
                statement.setLong(4, BILLING_ID);

                statement.executeUpdate();
            }
        }

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             "DELETE FROM transformation_budget"
                     )) {

            statement.executeUpdate();
        }
    }
}