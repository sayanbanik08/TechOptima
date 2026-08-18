package com.techoptima.algorithm.topology;

import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TopologicalSorterTest {

    private Application application(
            long id,
            String name,
            String cost,
            int benefit,
            Criticality criticality,
            List<Long> dependencies) {

        return new Application(
                id,
                name,
                new BigDecimal(cost),
                benefit,
                criticality,
                Department.OPERATIONS,
                dependencies
        );
    }

    @Test
    void shouldProduceValidDependencyOrder() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        "20.00",
                        80,
                        Criticality.HIGH,
                        List.of()
                );

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        "25.00",
                        90,
                        Criticality.HIGH,
                        List.of(1L)
                );

        Application reporting =
                application(
                        3L,
                        "Reporting",
                        "15.00",
                        70,
                        Criticality.MEDIUM,
                        List.of(2L)
                );

        TopologicalSortResult result =
                TopologicalSorter.sort(
                        List.of(
                                reporting,
                                analytics,
                                crm
                        )
                );

        assertTrue(result.isValid());

        assertEquals(
                List.of(crm, analytics, reporting),
                result.getOrderedApplications()
        );

        assertTrue(
                result.getCyclicApplicationIds().isEmpty()
        );
    }

    @Test
    void shouldHandleMultipleDependencies() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        "20.00",
                        80,
                        Criticality.HIGH,
                        List.of()
                );

        Application erp =
                application(
                        2L,
                        "ERP",
                        "40.00",
                        100,
                        Criticality.CRITICAL,
                        List.of()
                );

        Application reporting =
                application(
                        3L,
                        "Reporting",
                        "15.00",
                        70,
                        Criticality.HIGH,
                        List.of(1L, 2L)
                );

        TopologicalSortResult result =
                TopologicalSorter.sort(
                        List.of(
                                reporting,
                                erp,
                                crm
                        )
                );

        assertTrue(result.isValid());

        List<Application> ordered =
                result.getOrderedApplications();

        assertEquals(3, ordered.size());

        assertTrue(
                ordered.indexOf(crm)
                        < ordered.indexOf(reporting)
        );

        assertTrue(
                ordered.indexOf(erp)
                        < ordered.indexOf(reporting)
        );
    }

    @Test
    void shouldDetectCycle() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        "20.00",
                        80,
                        Criticality.HIGH,
                        List.of(2L)
                );

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        "25.00",
                        90,
                        Criticality.HIGH,
                        List.of(1L)
                );

        TopologicalSortResult result =
                TopologicalSorter.sort(
                        List.of(crm, analytics)
                );

        assertFalse(result.isValid());

        assertTrue(
                result.getOrderedApplications().isEmpty()
        );

        assertEquals(
                List.of(1L, 2L),
                result.getCyclicApplicationIds()
        );
    }

    @Test
    void shouldDetectPartialCycle() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        "20.00",
                        80,
                        Criticality.HIGH,
                        List.of()
                );

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        "25.00",
                        90,
                        Criticality.HIGH,
                        List.of(3L)
                );

        Application reporting =
                application(
                        3L,
                        "Reporting",
                        "15.00",
                        70,
                        Criticality.MEDIUM,
                        List.of(2L)
                );

        TopologicalSortResult result =
                TopologicalSorter.sort(
                        List.of(
                                crm,
                                analytics,
                                reporting
                        )
                );

        assertFalse(result.isValid());

        assertEquals(
                List.of(crm),
                result.getOrderedApplications()
        );

        assertEquals(
                List.of(2L, 3L),
                result.getCyclicApplicationIds()
        );
    }

    @Test
    void shouldUsePriorityWhenMultipleApplicationsAreReady() {

        Application low =
                application(
                        1L,
                        "LOW",
                        "10.00",
                        100,
                        Criticality.LOW,
                        List.of()
                );

        Application critical =
                application(
                        2L,
                        "CRITICAL",
                        "10.00",
                        10,
                        Criticality.CRITICAL,
                        List.of()
                );

        Application high =
                application(
                        3L,
                        "HIGH",
                        "10.00",
                        50,
                        Criticality.HIGH,
                        List.of()
                );

        TopologicalSortResult result =
                TopologicalSorter.sort(
                        List.of(low, high, critical)
                );

        assertTrue(result.isValid());

        assertEquals(
                List.of(critical, high, low),
                result.getOrderedApplications()
        );
    }

    @Test
    void shouldHandleEmptyInput() {

        TopologicalSortResult result =
                TopologicalSorter.sort(
                        List.of()
                );

        assertTrue(result.isValid());

        assertTrue(
                result.getOrderedApplications().isEmpty()
        );
    }

    @Test
    void shouldRejectNullInput() {

        assertThrows(
                IllegalArgumentException.class,
                () -> TopologicalSorter.sort(null)
        );
    }

    @Test
    void shouldRejectNullApplication() {

        List<Application> applications =
                new ArrayList<>();

        applications.add(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> TopologicalSorter.sort(
                        applications
                )
        );
    }

    @Test
    void shouldRejectDuplicateApplicationIds() {

        Application first =
                application(
                        1L,
                        "CRM",
                        "20.00",
                        80,
                        Criticality.HIGH,
                        List.of()
                );

        Application second =
                application(
                        1L,
                        "Different CRM",
                        "30.00",
                        90,
                        Criticality.CRITICAL,
                        List.of()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> TopologicalSorter.sort(
                        List.of(first, second)
                )
        );
    }

    @Test
    void shouldRejectMissingDependency() {

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        "25.00",
                        90,
                        Criticality.HIGH,
                        List.of(99L)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> TopologicalSorter.sort(
                        List.of(analytics)
                )
        );
    }

    @Test
    void shouldIgnoreDuplicateDependencyEntries() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        "20.00",
                        80,
                        Criticality.HIGH,
                        List.of()
                );

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        "25.00",
                        90,
                        Criticality.HIGH,
                        List.of(1L, 1L)
                );

        TopologicalSortResult result =
                TopologicalSorter.sort(
                        List.of(
                                analytics,
                                crm
                        )
                );

        assertTrue(result.isValid());

        assertEquals(
                List.of(crm, analytics),
                result.getOrderedApplications()
        );
    }

    @Test
    void shouldCorrectlyOrderDisconnectedSubgraphs() {

        Application crm =
                application(
                        1L,
                        "CRM",
                        "20.00",
                        80,
                        Criticality.HIGH,
                        List.of()
                );

        Application analytics =
                application(
                        2L,
                        "Analytics",
                        "25.00",
                        90,
                        Criticality.MEDIUM,
                        List.of(1L)
                );

        Application payment =
                application(
                        3L,
                        "Payment",
                        "30.00",
                        95,
                        Criticality.CRITICAL,
                        List.of()
                );

        Application billing =
                application(
                        4L,
                        "Billing",
                        "15.00",
                        70,
                        Criticality.HIGH,
                        List.of(3L)
                );

        TopologicalSortResult result =
                TopologicalSorter.sort(
                        List.of(analytics, billing, crm, payment)
                );

        assertTrue(result.isValid());

        List<Application> ordered =
                result.getOrderedApplications();

        assertEquals(4, ordered.size());

        // Prerequisite must precede dependent in Component 1
        assertTrue(ordered.indexOf(crm) < ordered.indexOf(analytics));

        // Prerequisite must precede dependent in Component 2
        assertTrue(ordered.indexOf(payment) < ordered.indexOf(billing));

        // Highest criticality root (Payment - CRITICAL) should come before lower criticality root (CRM - HIGH)
        assertTrue(ordered.indexOf(payment) < ordered.indexOf(crm));
    }
}