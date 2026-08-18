package com.techoptima.repository;

import com.techoptima.database.DatabaseConnection;
import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationRepositoryTest {

    private static final long CRM_ID = 900001L;
    private static final long ANALYTICS_ID = 900002L;
    private static final long ERP_ID = 900003L;

    private final ApplicationRepository repository =
            new ApplicationRepository();

    @BeforeEach
    void cleanTestDataBefore() throws Exception {
        cleanupTestData();
    }

    @AfterEach
    void cleanTestDataAfter() throws Exception {
        cleanupTestData();
    }

    @Test
    void shouldSaveAndFindApplication() throws Exception {

        Application crm = application(
                CRM_ID,
                "CRM",
                "20.00",
                80,
                Criticality.HIGH,
                Department.SALES,
                List.of()
        );

        repository.save(crm);

        Application loaded =
                repository.findById(CRM_ID);

        assertNotNull(loaded);
        assertEquals(CRM_ID, loaded.getApplicationId());
        assertEquals("CRM", loaded.getApplicationName());
        assertEquals(
                new BigDecimal("20.00"),
                loaded.getModernizationCost()
        );
        assertEquals(80, loaded.getBusinessBenefit());
        assertEquals(Criticality.HIGH, loaded.getCriticality());
        assertEquals(Department.SALES, loaded.getDepartment());
        assertTrue(
                loaded.getDependencyApplicationIds().isEmpty()
        );
    }

    @Test
    void shouldSaveAndLoadDependencies() throws Exception {

        Application crm = application(
                CRM_ID,
                "CRM",
                "20.00",
                80,
                Criticality.HIGH,
                Department.SALES,
                List.of()
        );

        Application analytics = application(
                ANALYTICS_ID,
                "Analytics",
                "25.00",
                90,
                Criticality.HIGH,
                Department.INFORMATION_TECHNOLOGY,
                List.of(CRM_ID)
        );

        repository.save(crm);
        repository.save(analytics);

        Application loaded =
                repository.findById(ANALYTICS_ID);

        assertNotNull(loaded);

        assertEquals(
                List.of(CRM_ID),
                loaded.getDependencyApplicationIds()
        );
    }

    @Test
    void shouldFindAllApplicationsWithDependencies() throws Exception {

        Application crm = application(
                CRM_ID,
                "CRM",
                "20.00",
                80,
                Criticality.HIGH,
                Department.SALES,
                List.of()
        );

        Application analytics = application(
                ANALYTICS_ID,
                "Analytics",
                "25.00",
                90,
                Criticality.HIGH,
                Department.INFORMATION_TECHNOLOGY,
                List.of(CRM_ID)
        );

        repository.save(crm);
        repository.save(analytics);

        List<Application> applications =
                repository.findAll();

        Application loadedAnalytics =
                applications.stream()
                        .filter(a ->
                                a.getApplicationId()
                                        == ANALYTICS_ID)
                        .findFirst()
                        .orElse(null);

        assertNotNull(loadedAnalytics);

        assertEquals(
                List.of(CRM_ID),
                loadedAnalytics.getDependencyApplicationIds()
        );
    }

    @Test
    void shouldUpdateApplicationAndDependencies() throws Exception {

        Application crm = application(
                CRM_ID,
                "CRM",
                "20.00",
                80,
                Criticality.HIGH,
                Department.SALES,
                List.of()
        );

        Application erp = application(
                ERP_ID,
                "ERP",
                "40.00",
                100,
                Criticality.CRITICAL,
                Department.OPERATIONS,
                List.of()
        );

        Application analytics = application(
                ANALYTICS_ID,
                "Analytics",
                "25.00",
                90,
                Criticality.HIGH,
                Department.INFORMATION_TECHNOLOGY,
                List.of(CRM_ID)
        );

        repository.save(crm);
        repository.save(erp);
        repository.save(analytics);

        Application updatedAnalytics = application(
                ANALYTICS_ID,
                "Analytics Modernized",
                "30.00",
                95,
                Criticality.CRITICAL,
                Department.INFORMATION_TECHNOLOGY,
                List.of(ERP_ID)
        );

        repository.update(updatedAnalytics);

        Application loaded =
                repository.findById(ANALYTICS_ID);

        assertNotNull(loaded);
        assertEquals(
                "Analytics Modernized",
                loaded.getApplicationName()
        );
        assertEquals(
                new BigDecimal("30.00"),
                loaded.getModernizationCost()
        );
        assertEquals(95, loaded.getBusinessBenefit());
        assertEquals(
                Criticality.CRITICAL,
                loaded.getCriticality()
        );
        assertEquals(
                List.of(ERP_ID),
                loaded.getDependencyApplicationIds()
        );
    }

    @Test
    void shouldDeleteApplicationWithoutDependencies()
            throws Exception {

        Application crm = application(
                CRM_ID,
                "CRM",
                "20.00",
                80,
                Criticality.HIGH,
                Department.SALES,
                List.of()
        );

        repository.save(crm);

        assertNotNull(repository.findById(CRM_ID));

        boolean deleted =
                repository.deleteById(CRM_ID);

        assertTrue(deleted);
        assertNull(repository.findById(CRM_ID));
    }

    @Test
    void shouldReturnNullForUnknownApplication()
            throws Exception {

        assertNull(repository.findById(999999999L));
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

    private void cleanupTestData() throws Exception {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM application_dependencies " +
                                 "WHERE application_id IN (?, ?, ?) " +
                                 "OR dependency_application_id IN (?, ?, ?)"
                         )) {

                statement.setLong(1, CRM_ID);
                statement.setLong(2, ANALYTICS_ID);
                statement.setLong(3, ERP_ID);
                statement.setLong(4, CRM_ID);
                statement.setLong(5, ANALYTICS_ID);
                statement.setLong(6, ERP_ID);

                statement.executeUpdate();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "DELETE FROM applications " +
                                 "WHERE application_id IN (?, ?, ?)"
                         )) {

                statement.setLong(1, CRM_ID);
                statement.setLong(2, ANALYTICS_ID);
                statement.setLong(3, ERP_ID);

                statement.executeUpdate();
            }
        }
    }
}